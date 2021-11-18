package org.codeplay.processor;

import org.codeplay.annotation.Factory;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import java.lang.reflect.Type;
import java.nio.file.ProviderMismatchException;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author coldilock
 */
public class FactoryAnnotatedClass {
    private TypeElement annotatedClassElement;
    private String qualifiedSuperClassName;
    private String simpleFactoryGroupName;
    private String id;

    public FactoryAnnotatedClass(TypeElement classElement) throws ProcessingException {
        this.annotatedClassElement = classElement;
        Factory annotation = classElement.getAnnotation(Factory.class);
        id = annotation.id();

        if(id.isEmpty()){
            throw new ProcessingException(classElement,
                    "id() in @%s for class %s is null or empty! that's not allowed",
                    Factory.class.getSimpleName(), classElement.getQualifiedName().toString());
        }

        try{
            Class<?> clazz = annotation.type();
            qualifiedSuperClassName = clazz.getCanonicalName();
            simpleFactoryGroupName = clazz.getSimpleName();
        } catch (MirroredTypeException mte){
            DeclaredType classTypeMirror = (DeclaredType) mte.getTypeMirror();
            TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
            qualifiedSuperClassName = classTypeElement.getQualifiedName().toString();
            simpleFactoryGroupName = classTypeElement.getSimpleName().toString();
        }
    }

    public String getId(){
        return id;
    }

    public String getQualifiedSuperClassName(){
        return simpleFactoryGroupName;
    }

    public TypeElement getTypeElement(){
        return annotatedClassElement;
    }


}
