package ai.amani.sdk.utils

/**
 * @Author: zekiamani
 * @Date: 6.09.2022
 */
annotation class Annotations {
    annotation class ProdEnvironmentCase(val reason: String)

    annotation class TestEnvironmentCase(val reason: String)
}
