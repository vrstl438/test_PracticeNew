package common.annotations;

import common.extensions.OpenAsUserExtensions;
import org.junit.jupiter.api.extension.ExtendWith;
import ui.actions.Pages;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ExtendWith(OpenAsUserExtensions.class)
public @interface OpenAsUser {
    Pages page();
}
