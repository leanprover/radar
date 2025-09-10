package org.leanlang.radar.server.config;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

public final class RepoNamesUniqueValidator implements ConstraintValidator<RepoNamesUnique, List<ServerConfigRepo>> {

    @Override
    public boolean isValid(@Nullable List<ServerConfigRepo> value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        var uniqueNames = value.stream().map(ServerConfigRepo::name).collect(Collectors.toSet());
        return uniqueNames.size() == value.size();
    }
}
