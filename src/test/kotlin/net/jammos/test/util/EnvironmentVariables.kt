package net.jammos.test.util

import java.util.*

object EnvironmentVariables {
    fun setEnv(newenv: Map<String, String>) {
        try {
            val processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment")
            val theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment")
            theEnvironmentField.isAccessible = true
            val env = theEnvironmentField.get(null) as MutableMap<String, String>
            env.putAll(newenv)
            val theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment")
            theCaseInsensitiveEnvironmentField.isAccessible = true
            val cienv = theCaseInsensitiveEnvironmentField.get(null) as MutableMap<String, String>
            cienv.putAll(newenv)
        } catch (e: NoSuchFieldException) {
            try {
                val classes = Collections::class.java!!.getDeclaredClasses()
                val env = System.getenv()
                for (cl in classes) {
                    if ("java.util.Collections\$UnmodifiableMap" == cl.getName()) {
                        val field = cl.getDeclaredField("m")
                        field.setAccessible(true)
                        val obj = field.get(env)
                        val map = obj as MutableMap<String, String>
                        map.clear()
                        map.putAll(newenv)
                    }
                }
            } catch (e2: Exception) {
                e2.printStackTrace()
            }

        } catch (e1: Exception) {
            e1.printStackTrace()
        }

    }
}