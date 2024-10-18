package models;

import gg.ingot.iron.annotations.Model;

/**
 * @param name The name of the user
 * @param age The age of the user
 * @param active The active status of the user
 */
// todo: KSP doesn't support records, we can't unit test these at the moment
@Model
public record UserRecord(
    String name,
    int age,
    boolean active
) {}