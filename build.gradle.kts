plugins {
    id("gg.meza.stonecraft")
    checkstyle
}

checkstyle {
    toolVersion = "10.20.2"
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
}
