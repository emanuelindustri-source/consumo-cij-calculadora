# Reglas ProGuard de la aplicación de consumo Videojet.
# La app no usa reflexión ni serialización externa: las reglas por defecto son suficientes.
-keep class com.calculadora.consumocij.data.** { *; }
