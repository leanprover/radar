package org.leanlang.radar.server.config;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.stream.Collectors;

public class RepoNamesUniqueValidator implements ConstraintValidator<RepoNamesUnique, List<ServerConfigRepo>> {

    @Override
    public boolean isValid(final List<ServerConfigRepo> value, final ConstraintValidatorContext context) {
        var uniqueNames = value.stream().map(ServerConfigRepo::name).collect(Collectors.toSet());
        return uniqueNames.size() == value.size();
    }
}
