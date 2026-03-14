package com.katsufit.seeds

import com.katsufit.models.personal.exercise.DefaultExercise
import com.katsufit.models.personal.exercise.DefaultExercises
import org.jetbrains.exposed.sql.transactions.transaction

object ExerciseSeeds {

    fun seedDefaultExercises() {
        transaction {
            // Só faz seed se a tabela estiver vazia
            if (DefaultExercise.count() > 0) return@transaction

            // ==========================================
            // PEITO
            // ==========================================
            createExercise("Supino Reto com Barra", "Peito", "Força", "Intermediário",
                "Deite no banco reto, segure a barra na largura dos ombros, desça até o peito e empurre para cima.",
                "Barra, Banco Reto")
            createExercise("Supino Inclinado com Halteres", "Peito", "Força", "Intermediário",
                "No banco inclinado a 30-45°, desça os halteres até o nível do peito e empurre para cima.",
                "Halteres, Banco Inclinado")
            createExercise("Crucifixo Reto", "Peito", "Isolamento", "Iniciante",
                "Deite no banco reto com halteres, braços estendidos. Abra os braços em arco e retorne.",
                "Halteres, Banco Reto")
            createExercise("Flexão de Braços", "Peito", "Força", "Iniciante",
                "Posição de prancha, mãos na largura dos ombros. Desça o corpo e empurre para cima.",
                "Peso Corporal")
            createExercise("Crossover na Polia", "Peito", "Isolamento", "Intermediário",
                "Em pé entre as polias, puxe os cabos para baixo e para dentro num arco.",
                "Polia / Cabo")
            createExercise("Pullover com Halter", "Peito", "Força", "Intermediário",
                "Deite transversal no banco, segure o halter acima do peito e leve atrás da cabeça.",
                "Halter, Banco")
            createExercise("Supino Declinado", "Peito", "Força", "Avançado",
                "Deite no banco declinado, desça a barra até o peito inferior e empurre.",
                "Barra, Banco Declinado")

            // ==========================================
            // COSTAS
            // ==========================================
            createExercise("Puxada Frontal", "Costas", "Força", "Iniciante",
                "Sentado na máquina, puxe a barra até a altura do peito, controlando a descida.",
                "Máquina de Puxada")
            createExercise("Remada Curvada com Barra", "Costas", "Força", "Intermediário",
                "Incline o tronco a 45°, puxe a barra em direção ao abdômen.",
                "Barra")
            createExercise("Remada Baixa (Sentado)", "Costas", "Força", "Iniciante",
                "Sentado na máquina, puxe o cabo em direção ao abdômen, cotovelos rentes ao corpo.",
                "Máquina / Cabo")
            createExercise("Pulldown com Triângulo", "Costas", "Isolamento", "Iniciante",
                "Na polia alta com pegada triângulo, puxe até o peito apertando as costas.",
                "Polia / Cabo")
            createExercise("Barra Fixa (Pull-up)", "Costas", "Força", "Avançado",
                "Pendure-se na barra com pegada pronada e puxe o corpo até o queixo ultrapassar a barra.",
                "Barra Fixa")
            createExercise("Remada Unilateral com Halter", "Costas", "Força", "Intermediário",
                "Apoie um joelho e mão no banco, puxe o halter até a cintura com o outro braço.",
                "Halter, Banco")

            // ==========================================
            // PERNAS
            // ==========================================
            createExercise("Agachamento Livre", "Pernas", "Força", "Intermediário",
                "Barra nas costas, pés na largura dos ombros. Agache até as coxas ficarem paralelas ao chão.",
                "Barra, Rack")
            createExercise("Leg Press 45°", "Pernas", "Força", "Iniciante",
                "Sentado na máquina, empurre a plataforma estendendo as pernas sem travar os joelhos.",
                "Máquina Leg Press")
            createExercise("Cadeira Extensora", "Pernas", "Isolamento", "Iniciante",
                "Sentado na máquina, estenda as pernas até a posição reta, focando no quadríceps.",
                "Máquina Extensora")
            createExercise("Mesa Flexora", "Pernas", "Isolamento", "Iniciante",
                "Deitado de bruços, flexione as pernas trazendo os calcanhares em direção ao glúteo.",
                "Máquina Flexora")
            createExercise("Stiff com Barra", "Pernas", "Força", "Intermediário",
                "Em pé, desça a barra mantendo as pernas semi-estendidas, sentindo o posterior da coxa.",
                "Barra")
            createExercise("Panturrilha em Pé", "Pernas", "Isolamento", "Iniciante",
                "Em pé na máquina, suba nas pontas dos pés contraindo a panturrilha.",
                "Máquina / Step")
            createExercise("Avanço (Lunge)", "Pernas", "Força", "Intermediário",
                "Dê um passo à frente, flexione ambos os joelhos a 90° e retorne.",
                "Halteres / Peso Corporal")
            createExercise("Hack Squat", "Pernas", "Força", "Intermediário",
                "Na máquina hack, agache empurrando a plataforma para cima.",
                "Máquina Hack")

            // ==========================================
            // OMBROS
            // ==========================================
            createExercise("Desenvolvimento Militar", "Ombros", "Força", "Intermediário",
                "Sentado ou em pé, empurre a barra/halteres acima da cabeça até estender os braços.",
                "Barra / Halteres")
            createExercise("Elevação Lateral", "Ombros", "Isolamento", "Iniciante",
                "Em pé, eleve os halteres lateralmente até a altura dos ombros.",
                "Halteres")
            createExercise("Elevação Frontal", "Ombros", "Isolamento", "Iniciante",
                "Em pé, eleve os halteres à frente até a altura dos ombros, alternando ou simultâneo.",
                "Halteres")
            createExercise("Remada Alta", "Ombros", "Força", "Intermediário",
                "Em pé, puxe a barra para cima rente ao corpo até a altura do queixo.",
                "Barra")
            createExercise("Face Pull", "Ombros", "Isolamento", "Iniciante",
                "Na polia alta com corda, puxe em direção ao rosto abrindo os cotovelos.",
                "Polia / Corda")

            // ==========================================
            // BRAÇOS
            // ==========================================
            createExercise("Rosca Direta com Barra", "Braços", "Isolamento", "Iniciante",
                "Em pé, flexione os braços subindo a barra sem mover os cotovelos.",
                "Barra / Barra W")
            createExercise("Rosca Martelo", "Braços", "Isolamento", "Iniciante",
                "Em pé, flexione os braços com halteres em pegada neutra (palmas voltadas para dentro).",
                "Halteres")
            createExercise("Rosca Concentrada", "Braços", "Isolamento", "Intermediário",
                "Sentado, apoie o cotovelo na coxa interna e flexione o halter.",
                "Halter")
            createExercise("Tríceps Pulley (Corda)", "Braços", "Isolamento", "Iniciante",
                "Na polia alta com corda, estenda os braços para baixo sem mover os cotovelos.",
                "Polia / Corda")
            createExercise("Tríceps Francês", "Braços", "Isolamento", "Intermediário",
                "Deitado ou sentado, desça o halter/barra atrás da cabeça e estenda.",
                "Halter / Barra")
            createExercise("Tríceps Testa", "Braços", "Isolamento", "Intermediário",
                "Deitado, desça a barra até a testa e estenda os braços.",
                "Barra / Barra W")

            // ==========================================
            // ABDÔMEN
            // ==========================================
            createExercise("Crunch (Abdominal)", "Abdômen", "Isolamento", "Iniciante",
                "Deitado, flexione o tronco contraindo o abdômen sem puxar o pescoço.",
                "Peso Corporal / Colchonete")
            createExercise("Prancha Isométrica", "Abdômen", "Isometria", "Iniciante",
                "Apoie-se nos antebraços e pontas dos pés, mantendo o corpo reto por tempo determinado.",
                "Peso Corporal")
            createExercise("Elevação de Pernas", "Abdômen", "Isolamento", "Intermediário",
                "Pendurado ou deitado, eleve as pernas retas até 90°.",
                "Barra Fixa / Colchonete")
            createExercise("Abdominal Infra", "Abdômen", "Isolamento", "Iniciante",
                "Deitado, eleve o quadril do chão contraindo a parte inferior do abdômen.",
                "Peso Corporal / Colchonete")
            createExercise("Russian Twist", "Abdômen", "Isolamento", "Intermediário",
                "Sentado com o tronco inclinado, gire o tronco de um lado para o outro com ou sem peso.",
                "Peso / Medicine Ball")

            // ==========================================
            // CARDIO
            // ==========================================
            createExercise("Corrida na Esteira", "Cardio", "Aeróbico", "Iniciante",
                "Corra na esteira ajustando velocidade e inclinação conforme seu nível.",
                "Esteira")
            createExercise("Bicicleta Ergométrica", "Cardio", "Aeróbico", "Iniciante",
                "Pedale na bicicleta ajustando a resistência para o seu nível.",
                "Bicicleta Ergométrica")
            createExercise("Elíptico (Transport)", "Cardio", "Aeróbico", "Iniciante",
                "Movimente braços e pernas no elíptico em ritmo constante.",
                "Elíptico")
            createExercise("Pular Corda", "Cardio", "Aeróbico", "Intermediário",
                "Pule corda mantendo ritmo constante, variando entre pulos simples e duplos.",
                "Corda")
            createExercise("HIIT (Intervalado)", "Cardio", "Aeróbico", "Avançado",
                "Alterne períodos de alta intensidade (30s) com recuperação (30-60s).",
                "Variado")

            // ==========================================
            // FULL BODY
            // ==========================================
            createExercise("Burpee", "Full Body", "Funcional", "Avançado",
                "Agache, coloque as mãos no chão, salte os pés para trás em prancha, faça flexão, volte e salte.",
                "Peso Corporal")
            createExercise("Kettlebell Swing", "Full Body", "Funcional", "Intermediário",
                "Segure o kettlebell com ambas as mãos, balance entre as pernas e projete para a frente até a altura dos ombros.",
                "Kettlebell")
            createExercise("Turkish Get-up", "Full Body", "Funcional", "Avançado",
                "Deitado com um braço estendido segurando peso, levante-se até ficar em pé mantendo o braço no alto.",
                "Kettlebell / Halter")
            createExercise("Agachamento com Salto", "Full Body", "Funcional", "Intermediário",
                "Faça um agachamento e ao subir, salte explosivamente.",
                "Peso Corporal")
            createExercise("Mountain Climber", "Full Body", "Funcional", "Intermediário",
                "Em posição de prancha, alterne trazendo os joelhos ao peito rapidamente.",
                "Peso Corporal")
        }
    }

    private fun createExercise(
        name: String,
        muscleGroup: String,
        category: String,
        difficulty: String,
        instructions: String,
        equipment: String
    ) {
        DefaultExercise.new {
            this.name = name
            this.muscleGroup = muscleGroup
            this.category = category
            this.difficulty = difficulty
            this.instructions = instructions
            this.equipment = equipment
        }
    }
}
