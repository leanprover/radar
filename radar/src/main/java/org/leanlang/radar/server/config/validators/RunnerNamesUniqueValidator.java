package org.leanlang.radar.server.config.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.server.config.ServerConfigRunner;

public final class RunnerNamesUniqueValidator
        implements ConstraintValidator<RunnerNamesUnique, List<ServerConfigRunner>> {

    @Override
    public boolean isValid(@Nullable List<ServerConfigRunner> value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        var uniqueNames = value.stream().map(ServerConfigRunner::name).collect(Collectors.toSet());
        return uniqueNames.size() == value.size();
    }
}
