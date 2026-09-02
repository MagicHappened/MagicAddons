plugins {
    id("dev.kikugie.stonecutter")
}

// which version the source tree is currently shaped for, and so which one the IDE compiles and the
// debug client runs. Switch with the "Set active project to <version>" task
stonecutter active "26.1.2" /* [SC] DO NOT EDIT */

// builds every version in one go, for checking a change has not broken the other one
stonecutter tasks {
    named("build")
}
