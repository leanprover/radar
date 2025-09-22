package org.leanlang.radar.server.config;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

public final class RepoRunNamesUniqueValidator
        implements ConstraintValidator<RepoRunNamesUnique, List<ServerConfigRepoRun>> {

    @Override
    public boolean isValid(@Nullable List<ServerConfigRepoRun> value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        var uniqueNames = value.stream().map(ServerConfigRepoRun::name).collect(Collectors.toSet());
        return uniqueNames.size() == value.size();
    }
}
