package gg.ingot.iron.test;

import gg.ingot.iron.annotations.Column;
import gg.ingot.iron.annotations.Model;

@SuppressWarnings({"MissingJavadoc"})
@Model(table = "java_user_record")
public record UserRecord(
    @Column(primaryKey = true, autoIncrement = true)
    int id,
    String name,
    int age
) {}