dependencies {
    api(project(":cloud-bukkit"))
    compileOnly("com.destroystokyo.paper", "paper-api", vers["paper-api"])
    compileOnly("com.destroystokyo.paper", "paper-mojangapi", vers["paper-api"])
    compileOnly("org.jetbrains", "annotations", vers["jb-annotations"])
    compileOnly("com.google.guava", "guava", vers["guava"])
}
