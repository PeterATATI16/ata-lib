package io.atalib.processor;

import io.atalib.processor.gen.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("io.atalib.annotation.AtaEntity")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class AtaEntityProcessor extends AbstractProcessor {

    // Annotation FQNs to NOT mirror onto generated DTOs
    private static final Set<String> EXCLUDED_ANNOTATION_PACKAGES = Set.of(
            "jakarta.persistence.",
            "lombok.",
            "org.springframework.data.annotation.",
            "org.hibernate."
    );

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) return false;

        TypeElement ataEntityType = processingEnv.getElementUtils()
                .getTypeElement("io.atalib.annotation.AtaEntity");
        if (ataEntityType == null) return false;

        for (Element element : roundEnv.getElementsAnnotatedWith(ataEntityType)) {
            if (element.getKind() != ElementKind.CLASS) continue;
            try {
                EntityModel model = buildModel((TypeElement) element);
                generateAll(model);
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "[ata-lib-processor] Failed to generate files for "
                                + element.getSimpleName() + ": " + e.getMessage(),
                        element);
            }
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // Model building
    // -------------------------------------------------------------------------

    private EntityModel buildModel(TypeElement typeElement) {
        String className   = typeElement.getSimpleName().toString();
        String packageName = processingEnv.getElementUtils()
                .getPackageOf(typeElement).getQualifiedName().toString();

        // Read @AtaEntity attribute values via AnnotationMirror (avoids loading AtaEntity.class)
        Map<String, Object> attrValues = readAnnotationValues(typeElement, "io.atalib.annotation.AtaEntity");

        String baseUrl = (String) attrValues.getOrDefault("baseUrl", "");
        @SuppressWarnings("unchecked")
        List<String> responseExcludeList = (List<String>) attrValues.getOrDefault("responseExclude", List.of());
        @SuppressWarnings("unchecked")
        List<String> requestExcludeList  = (List<String>) attrValues.getOrDefault("requestExclude", List.of());

        // Auto-detect the @Id field type by walking the entity's class hierarchy
        String idTypeFqn = detectIdTypeFqn(typeElement);
        TypeUtils.TypeResolution idTypeRes = TypeUtils.resolve(idTypeFqn);

        if (baseUrl.isBlank()) baseUrl = "/" + className.toLowerCase();

        Set<String> responseExclude = new HashSet<>(responseExcludeList);
        Set<String> requestExclude  = new HashSet<>(requestExcludeList);

        List<FieldModel> fields = buildFields(typeElement);

        return new EntityModel(className, packageName, baseUrl, fields, responseExclude, requestExclude,
                idTypeRes.displayName, new ArrayList<>(idTypeRes.imports));
    }

    private List<FieldModel> buildFields(TypeElement typeElement) {
        List<FieldModel> result = new ArrayList<>();
        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.FIELD) continue;
            VariableElement field = (VariableElement) enclosed;
            if (field.getModifiers().contains(Modifier.STATIC)) continue;

            String name = field.getSimpleName().toString();
            TypeMirror typeMirror = field.asType();
            TypeUtils.TypeResolution resolution = TypeUtils.resolve(typeMirror.toString());

            List<String> annotations = field.getAnnotationMirrors().stream()
                    .filter(m -> !isExcludedAnnotation(m.getAnnotationType().toString()))
                    .map(Object::toString)
                    .collect(Collectors.toList());

            result.add(new FieldModel(
                    name,
                    resolution.displayName,
                    new ArrayList<>(resolution.imports),
                    annotations));
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // File generation
    // -------------------------------------------------------------------------

    private void generateAll(EntityModel model) throws IOException {
        write(model.getPackageName() + "." + model.responseDtoName(),  DtoGenerator.generateResponse(model));
        write(model.getPackageName() + "." + model.requestDtoName(),   DtoGenerator.generateRequest(model));
        write(model.getPackageName() + "." + model.mapperName(),       MapperGenerator.generate(model));
        write(model.getPackageName() + "." + model.repositoryName(),   RepositoryGenerator.generate(model));
        write(model.getPackageName() + "." + model.serviceImplName(),  ServiceGenerator.generate(model));
        write(model.getPackageName() + "." + model.controllerName(),   ControllerGenerator.generate(model));
    }

    private void write(String qualifiedName, String content) throws IOException {
        JavaFileObject file = processingEnv.getFiler().createSourceFile(qualifiedName);
        try (PrintWriter writer = new PrintWriter(file.openWriter())) {
            writer.print(content);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Walks the class hierarchy to find the field annotated with @Id and returns its type FQN.
     * Falls back to "java.lang.Long" if not found.
     */
    private String detectIdTypeFqn(TypeElement typeElement) {
        TypeElement current = typeElement;
        while (current != null) {
            for (Element enclosed : current.getEnclosedElements()) {
                if (enclosed.getKind() != ElementKind.FIELD) continue;
                VariableElement field = (VariableElement) enclosed;
                boolean isId = field.getAnnotationMirrors().stream()
                        .anyMatch(m -> m.getAnnotationType().toString().equals("jakarta.persistence.Id"));
                if (isId) {
                    return field.asType().toString();
                }
            }
            TypeMirror superMirror = current.getSuperclass();
            if (superMirror == null || superMirror.getKind() == TypeKind.NONE) break;
            Element superElement = processingEnv.getTypeUtils().asElement(superMirror);
            if (!(superElement instanceof TypeElement)) break;
            current = (TypeElement) superElement;
            if ("java.lang.Object".equals(current.getQualifiedName().toString())) break;
        }
        return "java.lang.Long";
    }

    /**
     * Reads annotation element values by FQN without loading the annotation class.
     * Returns a map of attribute name → value (String for scalars, List<String> for arrays).
     */
    private Map<String, Object> readAnnotationValues(TypeElement element, String annotationFqn) {
        Map<String, Object> result = new HashMap<>();
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            if (!mirror.getAnnotationType().toString().equals(annotationFqn)) continue;
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry
                    : mirror.getElementValues().entrySet()) {
                String key = entry.getKey().getSimpleName().toString();
                Object raw = entry.getValue().getValue();
                if (raw instanceof List<?> list) {
                    List<String> values = list.stream()
                            .filter(v -> v instanceof AnnotationValue)
                            .map(v -> ((AnnotationValue) v).getValue().toString())
                            .collect(Collectors.toList());
                    result.put(key, values);
                } else {
                    result.put(key, raw.toString());
                }
            }
            break;
        }
        return result;
    }

    private boolean isExcludedAnnotation(String fqn) {
        return EXCLUDED_ANNOTATION_PACKAGES.stream().anyMatch(fqn::startsWith);
    }
}
