package models;

import gg.ingot.iron.annotations.Model;

import java.util.Optional;

/**
 * @param name The name of the user
 * @param age The optional age of the user
 * @param active The active status of the user
 */
@Model
public record UserOptional(
    String name,
    Optional<Integer> age,
    boolean active
) {}