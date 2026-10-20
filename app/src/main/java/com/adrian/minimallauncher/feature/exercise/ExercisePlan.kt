package com.adrian.minimallauncher.feature.exercise

import java.time.LocalDate

enum class SessionKind { STRENGTH, CARDIO, INTERVALS, REST }

/** One movement of the plan; [sets] are tapped one by one, [timed] ones count seconds instead of reps. */
data class PlanExercise(
    val id: String,
    val name: String,
    val sets: Int,
    val scheme: String,
    val how: String,
    val care: String,
    val core: Boolean = false,
    val timed: Boolean = false,
) {
    /** Lower end of the rep (or seconds) range, used as the starting value the first time. */
    val minTarget: Int get() = Regex("(\\d+)-").find(scheme)?.groupValues?.get(1)?.toInt() ?: 10
}

data class Session(
    val code: String,
    val title: String,
    val duration: String,
    val kind: SessionKind,
    val exercises: List<PlanExercise> = emptyList(),
    val note: String = "",
) {
    val totalSets: Int get() = exercises.sumOf { it.sets }
}

/** "Físico de atleta", the Tokyo plan (October 2026): three strength days, two conditioning days, two rest days. */
object TokyoGuide {
    val warmup = listOf("3 min suaves de bici o caminadora", "8 sentadillas sin peso", "8 bisagras de cadera", "Movilidad suave de tobillos y hombros")

    const val PROGRESSION = "Cuando llegues al máximo de repeticiones con buena técnica en dos sesiones, sube peso. Si ya usas 10 kg, prueba una bajada de 3 segundos."
    const val INTENSITY = "Termina cada serie con unas dos repeticiones buenas en reserva. Detente ante dolor articular o un tirón."

    private fun pushUps(sets: Int) = PlanExercise(
        "push_up", "Flexiones", sets, "$sets × 6-15",
        "Manos algo más separadas que los hombros. Cabeza, cadera y tobillos alineados. Baja el pecho y empuja el suelo.",
        "No dejes caer la cadera ni abras los codos por completo. Más fácil: en pared.",
    )
    private val row = PlanExercise(
        "db_row", "Remo con mancuerna", 3, "3 × 10-20",
        "Inclina el tronco desde la cadera y apoya la mano libre en el muslo. Lleva la mancuerna hacia la cadera y baja con control. Un brazo a la vez.",
        "No gires el torso ni encojas el hombro.",
    )
    private val curl = PlanExercise(
        "biceps_curl", "Curl de bíceps", 2, "2 × 10-15",
        "Palmas al frente y codos cerca del cuerpo. Sube sin mover los hombros y baja despacio.",
        "No te balancees ni adelantes los codos.",
    )
    private val triceps = PlanExercise(
        "triceps_ext", "Extensión de tríceps", 2, "2 × 10-15",
        "Una mancuerna ligera con ambas manos. Bájala detrás de la cabeza flexionando los codos y extiéndelos.",
        "Abdomen firme; no fuerces los hombros ni arquees la espalda.",
    )

    val week: List<Session> = listOf(
        Session(
            "A", "Pecho y espalda", "45-50 MIN", SessionKind.STRENGTH,
            listOf(
                pushUps(3),
                row,
                PlanExercise(
                    "static_lunge", "Zancada estática", 2, "2 × 8-12 POR LADO",
                    "Pies separados; baja verticalmente y sube.",
                    "Rodilla delantera alineada con el pie. Empieza sin peso.",
                ),
                curl,
                PlanExercise(
                    "dead_bug", "Dead bug", 2, "2 × 8-12 POR LADO",
                    "Boca arriba, caderas y rodillas a 90°. Extiende un brazo y la pierna contraria, vuelve y alterna. Exhala al extender.",
                    "No arquees la zona lumbar; reduce el recorrido si se despega.", core = true,
                ),
            ),
        ),
        Session("~", "Cardio suave", "25-35 MIN", SessionKind.CARDIO, note = "Bicicleta, elíptica o caminadora a un ritmo que te permita hablar en frases completas."),
        Session(
            "B", "Piernas, hombros y brazos", "45-50 MIN", SessionKind.STRENGTH,
            listOf(
                PlanExercise(
                    "romanian_deadlift", "Peso muerto rumano", 3, "3 × 10-15",
                    "Rodillas un poco flexionadas. Lleva la cadera atrás y desliza las mancuernas cerca de las piernas. Vuelve apretando glúteos.",
                    "No redondees la espalda ni lo conviertas en sentadilla.",
                ),
                PlanExercise(
                    "reverse_lunge", "Zancada hacia atrás", 2, "2 × 8-12 POR LADO",
                    "Desde de pie, da un paso atrás, baja y vuelve.",
                    "Rodilla delantera alineada con el pie; no pierdas el equilibrio.",
                ),
                PlanExercise(
                    "shoulder_press", "Press de hombros", 2, "2 × 8-15",
                    "Abdomen firme. Empuja las mancuernas por encima de la cabeza y baja con control.",
                    "No arquees la espalda para levantar más peso.",
                ),
                PlanExercise(
                    "lateral_raise", "Elevaciones laterales", 2, "2 × 12-20",
                    "Codos un poco flexionados; eleva los brazos a los lados hasta la altura del hombro.",
                    "No encojas los hombros ni lances las mancuernas.",
                ),
                PlanExercise(
                    "hammer_curl", "Curl martillo", 2, "2 × 10-15",
                    "Palmas mirándose todo el movimiento. Flexiona sin balancear el tronco y baja despacio.",
                    "No dobles las muñecas ni adelantes los codos.",
                ),
                triceps,
                PlanExercise(
                    "side_plank", "Plancha lateral", 2, "2 × 20-40 S POR LADO",
                    "De lado, codo debajo del hombro. Eleva la cadera en línea con hombro y pies.",
                    "No hundas el hombro ni gires el tronco. Más fácil: rodillas apoyadas.", core = true, timed = true,
                ),
            ),
        ),
        Session("·", "Descanso", "PASEO OPCIONAL", SessionKind.REST, note = "Recuperación. Si caminas, que sea suave. Duerme 7-9 horas."),
        Session(
            "C", "Pecho, espalda y brazos", "45-50 MIN", SessionKind.STRENGTH,
            listOf(
                PlanExercise(
                    "floor_press", "Press de pecho en el suelo", 3, "3 × 10-20",
                    "Boca arriba, rodillas flexionadas, codos a 30-60° del torso. Empuja sobre el pecho y baja hasta tocar suave el suelo.",
                    "No golpees los codos contra el suelo ni dobles las muñecas.",
                ),
                row,
                pushUps(2),
                curl,
                triceps,
                PlanExercise(
                    "calf_raise", "Elevación de talón a una pierna", 2, "2 × 12-20 POR LADO",
                    "Apóyate suave en una pared. Eleva el talón de la pierna de apoyo y baja lento.",
                    "No rebotes ni tuerzas el tobillo.",
                ),
                PlanExercise(
                    "reverse_crunch", "Crunch inverso", 2, "2 × 10-15",
                    "Boca arriba, rodillas flexionadas. Lleva las rodillas al pecho y despega un poco la pelvis. Baja despacio.",
                    "Sin impulso. Si molesta la espalda, reduce el recorrido o usa dead bug.", core = true,
                ),
            ),
        ),
        Session(
            "//", "Bici con intervalos", "22 MIN APROX.", SessionKind.INTERVALS,
            note = "5 min suaves + 6 rondas de 30 s fuerte y 90 s suave + 5 min suaves. Sin esfuerzo máximo; si estás retomando, haz cardio suave las primeras dos semanas.",
        ),
        Session("·", "Descanso", "RECUPERACIÓN", SessionKind.REST, note = "Día libre. Buen momento para medir la cintura."),
    )

    fun sessionFor(date: LocalDate): Session = week[date.dayOfWeek.value - 1]
}
