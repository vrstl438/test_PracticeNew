package common.annotations;

import common.extensions.CreateUserAndAccountExtensions;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ExtendWith(CreateUserAndAccountExtensions.class)
public @interface CreateUserAndAccount {
    int accountCount() default 1;
}