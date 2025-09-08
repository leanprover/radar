package org.leanlang.radar.server.config;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.stream.Collectors;

public class RepoNamesUniqueValidator implements ConstraintValidator<RepoNamesUnique, List<RadarConfigRepo>> {

    @Override
    public boolean isValid(List<RadarConfigRepo> value, ConstraintValidatorContext context) {
        var uniqueNames = value.stream().map(RadarConfigRepo::name).collect(Collectors.toSet());
        return uniqueNames.size() == value.size();
    }
}
