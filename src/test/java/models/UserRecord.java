package models;

import gg.ingot.iron.annotations.Model;

/**
 * @param name The name of the user
 * @param age The age of the user
 * @param active The active status of the user
 */
@Model
public record UserRecord(
    String name,
    int age,
    boolean active
) {}