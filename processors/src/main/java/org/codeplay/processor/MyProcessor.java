package org.codeplay.processor;


import org.codeplay.annotation.Factory;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author coldilock
 *
 * http://hannesdorfmann.com/annotation-processing/annotationprocessing101/
 *
 * the annotation processor runs in it’s own jvm
 * javac starts a complete java virtual machine for running annotation processors.
 */
public class MyProcessor extends AbstractProcessor {
    private Types typeUtils;
    private Elements elementUtils;
    private Filer filer;
    private Messager messager;
    private Map<String, FactoryGroupedClasses> factoryClasses = new LinkedHashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        typeUtils = processingEnv.getTypeUtils();
        // Element represents a program element such as a package, class, or method.
        elementUtils = processingEnv.getElementUtils();
        // with Filer you can create files
        filer = processingEnv.getFiler();
        //  A Messager provides the way for an annotation processor to report error messages, warnings and other notices.
        // Messager is used to write messages to the third party developer who uses your annotation processor in their projects.
        messager = processingEnv.getMessager();


    }

    public void checkValidClass(FactoryAnnotatedClass item) throws ProcessingException{
        // Cast to TypeElement, has more type specific methods
        TypeElement classElement = item.getTypeElement();
        if(!classElement.getModifiers().contains(Modifier.PUBLIC)){
            throw new ProcessingException(classElement, "The class %s is not public.",
                    classElement.getQualifiedName().toString());
        }

        // Check if it's an abstract class
        if(classElement.getModifiers().contains(Modifier.ABSTRACT)){
            throw new ProcessingException(classElement,
                    "The class %s is abstract. You can't annotate abstract classes with @%",
                    classElement.getQualifiedName().toString(), Factory.class.getSimpleName());
        }

        // Check inheritance: Class must be childclass as specified in @Factory.type();
        TypeElement superClassElement = elementUtils.getTypeElement(item.getQualifiedSuperClassName());
        if(superClassElement.getKind() == ElementKind.INTERFACE){
            // Check interface implemented
            if(!classElement.getInterfaces().contains(superClassElement.asType())) {
                throw new ProcessingException(classElement,
                        "The class %s annotated with @s must implement the interface %s",
                        classElement.getQualifiedName().toString(), Factory.class.getSimpleName(),
                        item.getQualifiedSuperClassName());
            }
        } else {
            // Check subclassing
            TypeElement currentClass = classElement;
            while(true){
                TypeMirror superClassType = currentClass.getSuperclass();
                if(superClassType.getKind() == TypeKind.NONE){
                    throw new ProcessingException(classElement,
                            "The class %s annotated with @%s must inherit from %s",
                            classElement.getQualifiedName().toString(), Factory.class.getSimpleName(),
                            item.getQualifiedSuperClassName());
                }

                if(superClassType.toString().equals(item.getQualifiedSuperClassName())){
                    // Required super class found
                    break;
                }

                // Moving up in inheritance tree
                currentClass = (TypeElement) typeUtils.asElement(superClassType);
            }
        }

        // Check if an empty public constructor is given
        for(Element enclosed : classElement.getEnclosedElements()){
            if(enclosed.getKind() == ElementKind.CONSTRUCTOR){
                ExecutableElement constructorElement = (ExecutableElement) enclosed;
                if(constructorElement.getParameters().size() == 0 && constructorElement.getModifiers().contains(Modifier.PUBLIC)){
                    // Found an empty constructor
                    return;
                }
            }
        }

        // No empty constructor found
        throw new ProcessingException(classElement,
                "The class %s must provide an public empty default constructor",
                classElement.getQualifiedName().toString());


    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {// Itearate over all @Factory annotated elements(Element can be a class, method, variable etc.)
            for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(Factory.class)) {
                // Check if a class has been annotated with @Factory
                if (annotatedElement.getKind() != ElementKind.CLASS) {
                    error(annotatedElement, "Only classes can be annotated with @%s",
                            Factory.class.getSimpleName());
                    // Exit processing
                    return true;
                }

                // We can cast it, because we know that it of ElementKind.CLASS
                TypeElement typeElement = (TypeElement) annotatedElement;

                FactoryAnnotatedClass annotatedClass = new FactoryAnnotatedClass(typeElement);

                checkValidClass(annotatedClass);

                // Everything is fine, so try to add
                FactoryGroupedClasses factoryClass = factoryClasses.get(annotatedClass.getQualifiedSuperClassName());
                if(factoryClass == null){
                    String qualifiedGroupName = annotatedClass.getQualifiedSuperClassName();
                    factoryClass = new FactoryGroupedClasses(qualifiedGroupName);
                    factoryClasses.put(qualifiedGroupName, factoryClass);
                }

                // Checks if id is conflicting with another @Factory annotated class with the same i
                factoryClass.add(annotatedClass);

            }

            // Generate code
            for(FactoryGroupedClasses factoryClass : factoryClasses.values()){
                factoryClass.generateCode(elementUtils, filer);
            }
            factoryClasses.clear();

        } catch (ProcessingException e){
            error(e.getElement(), e.getMessage());
        } catch (IOException e){
            error(null, e.getMessage());
        }

        return false;
    }

    /**
     * specify that @Factory is processed by this processor
     * @return
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<>();
        annotations.add(Factory.class.getCanonicalName());
        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    private void error(Element e, String msg, Object... args){
        messager.printMessage(
                Diagnostic.Kind.ERROR,
                String.format(msg, args),
                e
        );
    }
}
