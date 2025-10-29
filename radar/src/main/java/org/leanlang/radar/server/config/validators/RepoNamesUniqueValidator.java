package org.leanlang.radar.server.config.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.server.config.ServerConfigRepo;

public final class RepoNamesUniqueValidator implements ConstraintValidator<RepoNamesUnique, List<ServerConfigRepo>> {

    @Override
    public boolean isValid(@Nullable List<ServerConfigRepo> value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        var uniqueNames = value.stream().map(it -> it.name).collect(Collectors.toSet());
        return uniqueNames.size() == value.size();
    }
}
