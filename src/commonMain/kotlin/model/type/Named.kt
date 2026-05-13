package model.type

/**
 * Interface for parts of the model which need to be switched between a stable id for [model.Internal] version
 * and a name for the [model.Pretty] version.
 */
interface Named {
    var name: String?
}
