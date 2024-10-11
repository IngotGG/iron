package gg.ingot.iron.test;

import gg.ingot.iron.annotations.Column;
import gg.ingot.iron.annotations.Model;

@SuppressWarnings({"MissingJavadoc"})
@Model(table = "java_users")
public class Users {
    
    @Column(primaryKey = true, autoIncrement = true)
    private int id;
    private String name;
    private int age;
    
}