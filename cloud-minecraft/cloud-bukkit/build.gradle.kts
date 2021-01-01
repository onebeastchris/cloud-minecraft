dependencies {
    api(project(":cloud-core"))
    api(project(":cloud-brigadier"))
    api(project(":cloud-tasks"))
    compileOnly("org.bukkit", "bukkit", vers["bukkit"])
    compileOnly("me.lucko", "commodore", vers["commodore"])
    compileOnly("org.jetbrains", "annotations", vers["jb-annotations"])
    compileOnly("com.google.guava", "guava", vers["guava"])
}
