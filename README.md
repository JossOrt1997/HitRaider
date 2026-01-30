# Hit-Raider

**Hit-Raider** es un proyecto académico y fan-project inspirado en el universo de *Warhammer 40k*. Se trata de un videojuego **2D roguelike de acción** con enfoque en combate técnico, generación procedural y narrativa ambiental, desarrollado como un **MVP funcional** para demostrar capacidades técnicas y creativas.

---

## 📌 Ficha Técnica

- **Género:** Roguelike de acción / Combat-platformer  
- **Motor:** libGDX (Java)  
- **Plataforma:** PC (Desktop)  
- **Estilo visual:** 2D Pixel Art, estética grimdark gótica/industrial  
- **Estado:** Prototipo (MVP)

---

## 🏢 Equipo de Desarrollo

**Analítica Software**

- **Kenia Luna Gutiérrez** — UI/UX & Art Lead  
  - Diseño visual, sprites, animaciones y coherencia estética.
- **José Eduardo Ortiz Domínguez** — Lead Developer  
  - Arquitectura del sistema, físicas, lógica de juego y generación procedural.

---

## 🎯 Objetivo del Proyecto

Desarrollar un prototipo jugable que integre:

- Combate basado en hitboxes y máquinas de estados  
- Generación procedural de niveles  
- Enemigos con comportamientos diferenciados  
- Interfaz coherente con la narrativa grimdark  
- Rendimiento estable en equipos de gama media  

El enfoque del proyecto es **MVP**, priorizando sistemas centrales sobre contenido accesorio.

---

## ⚙️ Características Principales

- Combate dinámico con armas diferenciadas  
- Progresión de dificultad basada en habilidad  
- Biomas con identidad visual y mecánica propia  
- Narrativa ambiental sin cinemáticas extensas  
- Alta rejugabilidad gracias a la generación procedural  

---

## 🧩 Requerimientos del Sistema

### Funcionales
- Control preciso y responsivo del personaje
- Sistema de combate fluido contra múltiples enemigos
- Progresión de dificultad gradual y justa
- Variedad de enemigos, armas y escenarios

### No Funcionales
- Rendimiento estable (FPS constantes)
- Interfaz clara y legible
- Ambientación inmersiva (visual y sonora)
- Curva de aprendizaje progresiva

---

## 📖 Contexto Narrativo

Ambientado en el universo de **Warhammer 40k**, el juego se sitúa tras la apertura de la Gran Fisura.

- **Ubicación:** Oasis Prime, mundo agrícola del Segmentum Tempestus  
- **Rol del jugador:** Marine Espacial del Capítulo de los Salamandras  
- **Misión:** Operación *“Yunque de Prometeo”*  
- **Enemigos:** Fuerzas Eldar en búsqueda de artefactos ancestrales  

La historia se transmite de forma **ambiental**, mediante escenarios, eventos y fragmentos de información.

---

## 🌍 Zonas Principales

- **Los Jardines de Cristal**  
  Bioma natural corrompido por tecnología Eldar, con trampas y enemigos veloces.

- **El Sagrario de Hueso Espectral**  
  Fortaleza gótica donde se concentra el poder enemigo.

---

## 🧠 Modelado del Sistema

### Arquitectura General
El proyecto sigue una arquitectura modular orientada a sistemas, separando claramente:

- **Lógica de juego**
- **Renderizado**
- **Entrada del usuario**
- **IA y comportamiento**
- **Gestión de niveles**

Esto facilita la escalabilidad y el mantenimiento del código.

### Modelado del Gameplay
- **Máquinas de estados** para el jugador y enemigos  
  (idle, movimiento, ataque, daño, muerte)
- **Sistema de colisiones e hitboxes** para precisión en combate
- **Gestión de armas** con atributos diferenciados (daño, alcance, velocidad)

### Inteligencia Artificial
- IA básica basada en:
  - Estados
  - Distancia al jugador
  - Temporizadores y patrones simples
- Comportamientos diferenciados por tipo de enemigo

### Generación Procedural
- Niveles construidos a partir de:
  - Salas modulares
  - Reglas de conexión
  - Distribución controlada de enemigos y recompensas
- Garantiza rejugabilidad manteniendo coherencia espacial

### Físicas
- Implementación con **Box2D**
- Simulación de gravedad, colisiones y desplazamiento consistente

---

## 📊 Business Model Canvas (Resumen)

- **Segmento:** Jugadores de PC (15–35 años) interesados en acción y roguelikes  
- **Propuesta de valor:** Combate técnico desafiante en un entorno narrativo oscuro  
- **Canales:** Git, builds locales, documentación técnica  
- **Relación con usuarios:** Retroalimentación directa e iteración constante  
- **Recursos clave:** Equipo reducido, libGDX, Box2D  
- **Estructura de costos:** Hardware, software y tiempo de desarrollo  

---

## 🚧 Alcance y Limitaciones

### Incluye
- Dos biomas jugables
- Sistema de combate completo
- Enemigos con IA diferenciada

### No Incluye
- Multijugador
- Sistemas avanzados de guardado
- Contenido completo (enfoque MVP)

---

## 📅 Contexto Académico

- **Institución:** Universidad Tecnológica Fidel Velázquez  
- **Fecha:** Enero 2026  
- **Tipo:** Proyecto académico / Prototipo funcional

---

## ⚠️ Disclaimer

Este proyecto es un **fan-project sin fines comerciales**, inspirado en *Warhammer 40k*. Todos los derechos del universo pertenecen a sus respectivos propietarios.
