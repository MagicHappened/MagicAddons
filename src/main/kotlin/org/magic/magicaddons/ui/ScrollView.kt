package org.magic.magicaddons.ui

/** A screen part that scrolls: the edges of what is on screen, in the coordinates its widgets use. */
interface ScrollView {
    val viewLeft: Int
    val viewTop: Int
    val viewRight: Int
    val viewBottom: Int
}
