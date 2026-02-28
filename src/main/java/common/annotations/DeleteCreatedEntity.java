package common.annotations;

import common.extensions.CreateUserAndAccountExtensions;
import common.extensions.DeleteCreatedEntityExtensions;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ExtendWith(DeleteCreatedEntityExtensions.class)
public @interface DeleteCreatedEntity {
}
