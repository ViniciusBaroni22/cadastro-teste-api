package com.katsufit.seeds

import com.katsufit.models.*
import com.katsufit.models.nutritionist.*
import com.katsufit.models.shared.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

/**
 * Seeds de Templates de Anamnese Profissionais
 * 
 * Este arquivo contém modelos completos de anamnese utilizados por
 * nutricionistas e personal trainers no dia a dia.
 */
object AnamnesisSeeds {

    // UUID fixo para templates do sistema (não pertence a nenhum profissional específico)
    private val SYSTEM_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

    fun seedDefaultTemplates() {
        transaction {
            // Verifica se já existem templates padrão
            val existingTemplates = AnamnesisTemplates
                .select { AnamnesisTemplates.isDefault eq true }
                .count()

            if (existingTemplates > 0) {
                println("✅ Templates padrão já existem. Pulando seed...")
                return@transaction
            }

            println("🌱 Criando templates de anamnese padrão...")

            // ========================================
            // TEMPLATE 1: ANAMNESE NUTRICIONAL COMPLETA
            // ========================================
            createAnamneseNutricionalCompleta()

            // ========================================
            // TEMPLATE 2: ANAMNESE ESPORTIVA
            // ========================================
            createAnamneseEsportiva()

            // ========================================
            // TEMPLATE 3: RECORDATÓRIO ALIMENTAR 24H
            // ========================================
            createRecordatorio24h()

            // ========================================
            // TEMPLATE 4: ANAMNESE PRIMEIRA CONSULTA (SIMPLIFICADA)
            // ========================================
            createAnamnesesPrimeiraConsulta()

            // ========================================
            // TEMPLATE 5: AVALIAÇÃO COMPORTAMENTO ALIMENTAR
            // ========================================
            createAvaliacaoComportamentoAlimentar()

            println("✅ Templates padrão criados com sucesso!")
        }
    }

    // ========================================
    // TEMPLATE 1: ANAMNESE NUTRICIONAL COMPLETA
    // ========================================
    private fun createAnamneseNutricionalCompleta() {
        val templateId = AnamnesisTemplates.insert {
            it[professionalId] = SYSTEM_UUID
            it[name] = "Anamnese Nutricional Completa"
            it[description] = "Modelo completo para primeira consulta nutricional. Inclui histórico clínico, hábitos alimentares, recordatório e avaliação detalhada."
            it[isDefault] = true
            it[isActive] = true
        }[AnamnesisTemplates.id].value

        // ----- SEÇÃO 1: DADOS PESSOAIS -----
        val secaoDadosPessoais = createSection(templateId, "Dados Pessoais", "Informações básicas do paciente", 0)
        
        createQuestion(secaoDadosPessoais, "Qual sua data de nascimento?", "DATE", true, 0)
        createQuestion(secaoDadosPessoais, "Qual seu sexo biológico?", "SINGLE_CHOICE", true, 1,
            options = listOf("Masculino", "Feminino"))
        createQuestion(secaoDadosPessoais, "Qual seu estado civil?", "SINGLE_CHOICE", false, 2,
            options = listOf("Solteiro(a)", "Casado(a)", "Divorciado(a)", "Viúvo(a)", "União Estável"))
        createQuestion(secaoDadosPessoais, "Qual sua profissão/ocupação?", "TEXT_SHORT", true, 3,
            placeholder = "Ex: Engenheiro, Professor, Estudante...")
        createQuestion(secaoDadosPessoais, "Quantas horas você trabalha por dia?", "NUMBER", false, 4,
            placeholder = "Ex: 8")
        createQuestion(secaoDadosPessoais, "Seu trabalho é predominantemente:", "SINGLE_CHOICE", false, 5,
            options = listOf("Sentado (escritório)", "Em pé", "Ativo/Movimento constante", "Trabalho braçal pesado", "Home office"))

        // ----- SEÇÃO 2: OBJETIVO E EXPECTATIVAS -----
        val secaoObjetivo = createSection(templateId, "Objetivo e Expectativas", "Entender as metas do paciente", 1)
        
        createQuestion(secaoObjetivo, "Qual seu principal objetivo com o acompanhamento nutricional?", "MULTIPLE_CHOICE", true, 0,
            options = listOf(
                "Emagrecimento",
                "Ganho de massa muscular",
                "Melhora da saúde geral",
                "Controle de doença (diabetes, colesterol, etc)",
                "Performance esportiva",
                "Reeducação alimentar",
                "Ganho de peso saudável",
                "Melhora da disposição/energia",
                "Melhora da qualidade do sono",
                "Redução de inchaço/retenção",
                "Outro"
            ))
        createQuestion(secaoObjetivo, "Se marcou 'Outro', especifique:", "TEXT_SHORT", false, 1)
        createQuestion(secaoObjetivo, "Qual seu peso desejado (meta)?", "NUMBER", false, 2,
            placeholder = "Em kg", helpText = "Deixe em branco se não tiver uma meta específica")
        createQuestion(secaoObjetivo, "Em quanto tempo você espera atingir seu objetivo?", "SINGLE_CHOICE", false, 3,
            options = listOf("1 mês", "3 meses", "6 meses", "1 ano", "Sem prazo definido"))
        createQuestion(secaoObjetivo, "Já fez acompanhamento nutricional antes?", "YES_NO", true, 4)
        createQuestion(secaoObjetivo, "Se sim, por quanto tempo e qual foi o resultado?", "TEXT_LONG", false, 5)

        // ----- SEÇÃO 3: HISTÓRICO CLÍNICO -----
        val secaoHistoricoClinico = createSection(templateId, "Histórico Clínico", "Condições de saúde e histórico médico", 2)
        
        createQuestion(secaoHistoricoClinico, "Possui alguma doença diagnosticada?", "MULTIPLE_CHOICE", true, 0,
            options = listOf(
                "Nenhuma",
                "Diabetes Tipo 1",
                "Diabetes Tipo 2",
                "Pré-diabetes",
                "Hipertensão",
                "Colesterol alto",
                "Triglicerídeos alto",
                "Hipotireoidismo",
                "Hipertireoidismo",
                "Síndrome do Ovário Policístico (SOP)",
                "Gastrite",
                "Refluxo gastroesofágico",
                "Síndrome do Intestino Irritável",
                "Doença Celíaca",
                "Intolerância à lactose",
                "Esteatose hepática (gordura no fígado)",
                "Anemia",
                "Depressão/Ansiedade",
                "Câncer (atual ou passado)",
                "Doença cardiovascular",
                "Doença renal",
                "Outra"
            ))
        createQuestion(secaoHistoricoClinico, "Se marcou 'Outra', especifique:", "TEXT_SHORT", false, 1)
        createQuestion(secaoHistoricoClinico, "Já fez alguma cirurgia? Se sim, quais?", "TEXT_LONG", false, 2,
            placeholder = "Ex: Apendicectomia em 2015, Cesariana em 2018...")
        createQuestion(secaoHistoricoClinico, "Já fez cirurgia bariátrica?", "SINGLE_CHOICE", false, 3,
            options = listOf("Não", "Sim - Bypass", "Sim - Sleeve", "Sim - Banda gástrica", "Sim - Outro tipo"))
        createQuestion(secaoHistoricoClinico, "Utiliza algum medicamento de uso contínuo?", "YES_NO", true, 4)
        createQuestion(secaoHistoricoClinico, "Se sim, liste os medicamentos e dosagens:", "TEXT_LONG", false, 5,
            placeholder = "Ex: Metformina 850mg 2x/dia, Losartana 50mg 1x/dia...")
        createQuestion(secaoHistoricoClinico, "Utiliza algum suplemento alimentar?", "YES_NO", false, 6)
        createQuestion(secaoHistoricoClinico, "Se sim, quais suplementos?", "TEXT_LONG", false, 7,
            placeholder = "Ex: Whey protein, Creatina, Vitamina D...")
        createQuestion(secaoHistoricoClinico, "Possui alguma alergia alimentar?", "YES_NO", true, 8)
        createQuestion(secaoHistoricoClinico, "Se sim, a quais alimentos?", "TEXT_SHORT", false, 9,
            placeholder = "Ex: Amendoim, frutos do mar, ovo...")
        createQuestion(secaoHistoricoClinico, "Possui alguma intolerância alimentar?", "YES_NO", false, 10)
        createQuestion(secaoHistoricoClinico, "Se sim, a quais alimentos?", "TEXT_SHORT", false, 11,
            placeholder = "Ex: Lactose, glúten...")

        // ----- SEÇÃO 4: HISTÓRICO FAMILIAR -----
        val secaoHistoricoFamiliar = createSection(templateId, "Histórico Familiar", "Doenças na família", 3)
        
        createQuestion(secaoHistoricoFamiliar, "Há histórico de quais doenças na sua família (pais, avós, irmãos)?", "MULTIPLE_CHOICE", false, 0,
            options = listOf(
                "Nenhuma relevante",
                "Diabetes",
                "Hipertensão",
                "Obesidade",
                "Doenças cardíacas",
                "Colesterol alto",
                "Câncer",
                "Doenças da tireoide",
                "Doenças renais",
                "AVC (derrame)",
                "Alzheimer/Demência",
                "Outra"
            ))
        createQuestion(secaoHistoricoFamiliar, "Se marcou 'Outra', especifique:", "TEXT_SHORT", false, 1)

        // ----- SEÇÃO 5: HÁBITOS DE VIDA -----
        val secaoHabitos = createSection(templateId, "Hábitos de Vida", "Sono, atividade física, tabagismo e outros hábitos", 4)
        
        createQuestion(secaoHabitos, "Quantas horas você dorme por noite em média?", "SINGLE_CHOICE", true, 0,
            options = listOf("Menos de 5 horas", "5-6 horas", "6-7 horas", "7-8 horas", "Mais de 8 horas"))
        createQuestion(secaoHabitos, "Como você classifica a qualidade do seu sono?", "SCALE", true, 1,
            helpText = "1 = Péssimo, 10 = Excelente", minValue = 1, maxValue = 10)
        createQuestion(secaoHabitos, "Tem dificuldade para dormir ou acorda durante a noite?", "YES_NO", false, 2)
        createQuestion(secaoHabitos, "Pratica atividade física regularmente?", "YES_NO", true, 3)
        createQuestion(secaoHabitos, "Se sim, quais atividades pratica?", "MULTIPLE_CHOICE", false, 4,
            options = listOf(
                "Musculação",
                "Corrida",
                "Caminhada",
                "Natação",
                "Ciclismo",
                "CrossFit",
                "Yoga/Pilates",
                "Lutas",
                "Dança",
                "Esportes coletivos",
                "Funcional",
                "Outro"
            ))
        createQuestion(secaoHabitos, "Com que frequência pratica exercícios?", "SINGLE_CHOICE", false, 5,
            options = listOf("1x por semana", "2x por semana", "3x por semana", "4x por semana", "5x por semana", "6x ou mais por semana"))
        createQuestion(secaoHabitos, "Qual o horário habitual dos treinos?", "SINGLE_CHOICE", false, 6,
            options = listOf("Manhã (antes das 9h)", "Manhã (9h-12h)", "Almoço (12h-14h)", "Tarde (14h-18h)", "Noite (após 18h)"))
        createQuestion(secaoHabitos, "Você fuma?", "SINGLE_CHOICE", true, 7,
            options = listOf("Nunca fumei", "Ex-fumante", "Sim, ocasionalmente", "Sim, diariamente"))
        createQuestion(secaoHabitos, "Consome bebidas alcoólicas?", "SINGLE_CHOICE", true, 8,
            options = listOf("Não bebo", "Raramente (eventos)", "1-2x por semana", "3-4x por semana", "Diariamente"))
        createQuestion(secaoHabitos, "Qual seu nível de estresse atualmente?", "SCALE", true, 9,
            helpText = "1 = Tranquilo, 10 = Muito estressado", minValue = 1, maxValue = 10)
        createQuestion(secaoHabitos, "Bebe água regularmente?", "YES_NO", true, 10)
        createQuestion(secaoHabitos, "Quantos litros de água bebe por dia aproximadamente?", "SINGLE_CHOICE", false, 11,
            options = listOf("Menos de 500ml", "500ml - 1 litro", "1 - 1,5 litros", "1,5 - 2 litros", "2 - 3 litros", "Mais de 3 litros"))

        // ----- SEÇÃO 6: HISTÓRICO DE PESO -----
        val secaoHistoricoPeso = createSection(templateId, "Histórico de Peso", "Evolução do peso ao longo da vida", 5)
        
        createQuestion(secaoHistoricoPeso, "Qual seu peso atual (em kg)?", "NUMBER", true, 0)
        createQuestion(secaoHistoricoPeso, "Qual sua altura (em cm)?", "NUMBER", true, 1)
        createQuestion(secaoHistoricoPeso, "Qual foi seu peso máximo na vida adulta (em kg)?", "NUMBER", false, 2)
        createQuestion(secaoHistoricoPeso, "Qual foi seu peso mínimo na vida adulta (em kg)?", "NUMBER", false, 3)
        createQuestion(secaoHistoricoPeso, "Com que idade você começou a ter problemas com peso (se aplicável)?", "TEXT_SHORT", false, 4)
        createQuestion(secaoHistoricoPeso, "Já fez dietas restritivas antes?", "YES_NO", false, 5)
        createQuestion(secaoHistoricoPeso, "Se sim, quais dietas já fez?", "MULTIPLE_CHOICE", false, 6,
            options = listOf(
                "Low carb",
                "Cetogênica",
                "Jejum intermitente",
                "Dieta da proteína",
                "Dieta de pontos",
                "Shakes substitutos de refeição",
                "Medicamentos para emagrecer",
                "Contagem de calorias",
                "Dieta paleolítica",
                "Vegetariana/Vegana",
                "Outra"
            ))
        createQuestion(secaoHistoricoPeso, "Como foi sua experiência com essas dietas?", "TEXT_LONG", false, 7,
            placeholder = "Conseguiu manter? Recuperou o peso? Como se sentiu?")

        // ----- SEÇÃO 7: HÁBITOS ALIMENTARES -----
        val secaoHabitosAlimentares = createSection(templateId, "Hábitos Alimentares", "Padrões e comportamentos alimentares", 6)
        
        createQuestion(secaoHabitosAlimentares, "Quantas refeições você faz por dia?", "SINGLE_CHOICE", true, 0,
            options = listOf("1-2 refeições", "3 refeições", "4-5 refeições", "6 ou mais refeições"))
        createQuestion(secaoHabitosAlimentares, "Quais refeições você costuma fazer?", "MULTIPLE_CHOICE", true, 1,
            options = listOf(
                "Café da manhã",
                "Lanche da manhã",
                "Almoço",
                "Lanche da tarde",
                "Jantar",
                "Ceia"
            ))
        createQuestion(secaoHabitosAlimentares, "Costuma pular refeições?", "SINGLE_CHOICE", true, 2,
            options = listOf("Nunca", "Raramente", "Às vezes", "Frequentemente", "Sempre"))
        createQuestion(secaoHabitosAlimentares, "Qual refeição você mais pula?", "SINGLE_CHOICE", false, 3,
            options = listOf("Café da manhã", "Almoço", "Jantar", "Lanches intermediários"))
        createQuestion(secaoHabitosAlimentares, "Onde você costuma fazer as refeições principais?", "MULTIPLE_CHOICE", true, 4,
            options = listOf(
                "Em casa - cozinho",
                "Em casa - delivery/pronto",
                "Restaurante por quilo",
                "Restaurante à la carte",
                "Fast food",
                "Marmita/comida levada de casa",
                "Refeitório da empresa"
            ))
        createQuestion(secaoHabitosAlimentares, "Quem prepara suas refeições?", "SINGLE_CHOICE", false, 5,
            options = listOf("Eu mesmo(a)", "Cônjuge/parceiro(a)", "Pais/familiares", "Empregada/cozinheira", "Compro pronto"))
        createQuestion(secaoHabitosAlimentares, "Come assistindo TV, celular ou trabalhando?", "SINGLE_CHOICE", true, 6,
            options = listOf("Nunca", "Raramente", "Às vezes", "Frequentemente", "Sempre"))
        createQuestion(secaoHabitosAlimentares, "Como você descreveria sua velocidade ao comer?", "SINGLE_CHOICE", true, 7,
            options = listOf("Muito devagar", "Devagar", "Normal", "Rápido", "Muito rápido"))
        createQuestion(secaoHabitosAlimentares, "Mastiga bem os alimentos?", "SINGLE_CHOICE", false, 8,
            options = listOf("Sim, sempre", "Na maioria das vezes", "Às vezes", "Raramente", "Não"))
        createQuestion(secaoHabitosAlimentares, "Tem compulsão alimentar ou episódios de comer exageradamente?", "SINGLE_CHOICE", true, 9,
            options = listOf("Nunca", "Raramente", "Às vezes", "Frequentemente", "Sempre"))
        createQuestion(secaoHabitosAlimentares, "Em quais situações você come mais do que deveria?", "MULTIPLE_CHOICE", false, 10,
            options = listOf(
                "Estresse/ansiedade",
                "Tristeza",
                "Tédio",
                "Comemorações/eventos",
                "TPM",
                "Noite/madrugada",
                "Finais de semana",
                "Quando estou sozinho(a)",
                "Não tenho esse comportamento"
            ))

        // ----- SEÇÃO 8: PREFERÊNCIAS E AVERSÕES -----
        val secaoPreferencias = createSection(templateId, "Preferências e Aversões Alimentares", "Alimentos que gosta e não gosta", 7)
        
        createQuestion(secaoPreferencias, "Quais alimentos você NÃO gosta ou não come de jeito nenhum?", "TEXT_LONG", true, 0,
            placeholder = "Liste os alimentos que você não consegue comer...")
        createQuestion(secaoPreferencias, "Quais são seus alimentos favoritos?", "TEXT_LONG", false, 1,
            placeholder = "Liste seus alimentos preferidos...")
        createQuestion(secaoPreferencias, "Você segue alguma restrição alimentar por escolha?", "MULTIPLE_CHOICE", false, 2,
            options = listOf(
                "Nenhuma",
                "Vegetariano",
                "Vegano",
                "Sem glúten",
                "Sem lactose",
                "Sem açúcar",
                "Low carb",
                "Kosher",
                "Halal",
                "Outra"
            ))
        createQuestion(secaoPreferencias, "Consome frutas regularmente?", "SINGLE_CHOICE", true, 3,
            options = listOf("Não como frutas", "1-2x por semana", "3-4x por semana", "Diariamente", "Várias vezes ao dia"))
        createQuestion(secaoPreferencias, "Consome verduras e legumes regularmente?", "SINGLE_CHOICE", true, 4,
            options = listOf("Não como", "1-2x por semana", "3-4x por semana", "Diariamente", "Em todas as refeições"))
        createQuestion(secaoPreferencias, "Consome doces com que frequência?", "SINGLE_CHOICE", true, 5,
            options = listOf("Não como doces", "Raramente", "1-2x por semana", "3-4x por semana", "Diariamente", "Várias vezes ao dia"))
        createQuestion(secaoPreferencias, "Consome frituras com que frequência?", "SINGLE_CHOICE", true, 6,
            options = listOf("Não como frituras", "Raramente", "1-2x por semana", "3-4x por semana", "Diariamente"))
        createQuestion(secaoPreferencias, "Consome refrigerante ou sucos industrializados?", "SINGLE_CHOICE", true, 7,
            options = listOf("Não consumo", "Raramente", "1-2x por semana", "3-4x por semana", "Diariamente"))
        createQuestion(secaoPreferencias, "Consome fast food com que frequência?", "SINGLE_CHOICE", true, 8,
            options = listOf("Não consumo", "Raramente", "1-2x por semana", "3-4x por semana", "Diariamente"))

        // ----- SEÇÃO 9: SINTOMAS GASTROINTESTINAIS -----
        val secaoGastro = createSection(templateId, "Saúde Gastrointestinal", "Sintomas e funcionamento intestinal", 8)
        
        createQuestion(secaoGastro, "Como está seu funcionamento intestinal?", "SINGLE_CHOICE", true, 0,
            options = listOf("Normal (1x ao dia)", "Constipado (menos de 3x por semana)", "Diarreia frequente", "Irregular/Alternado"))
        createQuestion(secaoGastro, "Sente algum destes sintomas frequentemente?", "MULTIPLE_CHOICE", true, 1,
            options = listOf(
                "Nenhum",
                "Azia/queimação",
                "Refluxo",
                "Gases/flatulência",
                "Distensão abdominal (barriga inchada)",
                "Náuseas",
                "Dor abdominal",
                "Cólicas",
                "Sensação de digestão lenta",
                "Arrotos frequentes"
            ))
        createQuestion(secaoGastro, "Algum alimento específico causa desconforto?", "TEXT_LONG", false, 2,
            placeholder = "Ex: Leite causa gases, feijão dá cólica...")

        // ----- SEÇÃO 10: SAÚDE DA MULHER (condicional) -----
        val secaoMulher = createSection(templateId, "Saúde da Mulher", "Questões específicas do público feminino (pular se masculino)", 9)
        
        createQuestion(secaoMulher, "Esta seção se aplica a você?", "YES_NO", true, 0,
            helpText = "Marque 'Sim' se for do sexo feminino")
        createQuestion(secaoMulher, "Seu ciclo menstrual é regular?", "SINGLE_CHOICE", false, 1,
            options = listOf("Sim", "Não", "Uso anticoncepcional contínuo", "Menopausa"))
        createQuestion(secaoMulher, "Sente alterações no apetite durante o ciclo/TPM?", "YES_NO", false, 2)
        createQuestion(secaoMulher, "Está grávida ou amamentando?", "SINGLE_CHOICE", false, 3,
            options = listOf("Não", "Grávida - 1º trimestre", "Grávida - 2º trimestre", "Grávida - 3º trimestre", "Amamentando"))
        createQuestion(secaoMulher, "Tem ou teve Síndrome do Ovário Policístico (SOP)?", "YES_NO", false, 4)

        // ----- SEÇÃO 11: EXAMES RECENTES -----
        val secaoExames = createSection(templateId, "Exames Recentes", "Resultados de exames laboratoriais", 10)
        
        createQuestion(secaoExames, "Fez exames de sangue recentemente (últimos 6 meses)?", "YES_NO", true, 0)
        createQuestion(secaoExames, "Se sim, pode informar os principais resultados?", "TEXT_LONG", false, 1,
            placeholder = "Ex: Glicose: 95, Colesterol total: 210, HDL: 45, LDL: 140, Triglicerídeos: 150, Hemoglobina: 13...")
        createQuestion(secaoExames, "Tem algum exame alterado que precisa de atenção?", "TEXT_LONG", false, 2)

        // ----- SEÇÃO 12: INFORMAÇÕES ADICIONAIS -----
        val secaoAdicional = createSection(templateId, "Informações Adicionais", "Outras informações relevantes", 11)
        
        createQuestion(secaoAdicional, "Há algo mais que gostaria de compartilhar sobre sua saúde ou alimentação?", "TEXT_LONG", false, 0)
        createQuestion(secaoAdicional, "Tem alguma dúvida específica para o nutricionista?", "TEXT_LONG", false, 1)
        createQuestion(secaoAdicional, "Como conheceu nosso consultório/clínica?", "SINGLE_CHOICE", false, 2,
            options = listOf("Google", "Instagram", "Indicação de amigo/familiar", "Indicação médica", "Convênio", "Outro"))

        println("   ✅ Template 'Anamnese Nutricional Completa' criado")
    }

    // ========================================
    // TEMPLATE 2: ANAMNESE ESPORTIVA
    // ========================================
    private fun createAnamneseEsportiva() {
        val templateId = AnamnesisTemplates.insert {
            it[professionalId] = SYSTEM_UUID
            it[name] = "Anamnese Esportiva"
            it[description] = "Modelo focado em atletas e praticantes de atividade física. Ideal para personal trainers e nutricionistas esportivos."
            it[isDefault] = true
            it[isActive] = true
        }[AnamnesisTemplates.id].value

        // ----- SEÇÃO 1: DADOS DO ATLETA -----
        val secaoDados = createSection(templateId, "Dados do Atleta", "Informações básicas", 0)
        
        createQuestion(secaoDados, "Data de nascimento", "DATE", true, 0)
        createQuestion(secaoDados, "Sexo", "SINGLE_CHOICE", true, 1, options = listOf("Masculino", "Feminino"))
        createQuestion(secaoDados, "Peso atual (kg)", "NUMBER", true, 2)
        createQuestion(secaoDados, "Altura (cm)", "NUMBER", true, 3)
        createQuestion(secaoDados, "Percentual de gordura corporal (se souber)", "NUMBER", false, 4)
        createQuestion(secaoDados, "Circunferência da cintura (cm)", "NUMBER", false, 5)

        // ----- SEÇÃO 2: MODALIDADE E TREINO -----
        val secaoTreino = createSection(templateId, "Modalidade e Treino", "Informações sobre a prática esportiva", 1)
        
        createQuestion(secaoTreino, "Qual(is) modalidade(s) você pratica?", "MULTIPLE_CHOICE", true, 0,
            options = listOf(
                "Musculação/Hipertrofia",
                "CrossFit",
                "Corrida",
                "Ciclismo",
                "Triathlon",
                "Natação",
                "Lutas (MMA, Jiu-jitsu, Boxe, etc)",
                "Futebol",
                "Basquete",
                "Vôlei",
                "Tênis",
                "Funcional",
                "Calistenia",
                "Powerlifting",
                "Bodybuilding",
                "Outro"
            ))
        createQuestion(secaoTreino, "Há quanto tempo pratica essa(s) modalidade(s)?", "SINGLE_CHOICE", true, 1,
            options = listOf("Menos de 6 meses", "6 meses - 1 ano", "1-2 anos", "2-5 anos", "Mais de 5 anos"))
        createQuestion(secaoTreino, "Qual sua frequência semanal de treino?", "SINGLE_CHOICE", true, 2,
            options = listOf("1-2x", "3x", "4x", "5x", "6x", "7x ou mais"))
        createQuestion(secaoTreino, "Duração média de cada treino", "SINGLE_CHOICE", true, 3,
            options = listOf("30-45 min", "45-60 min", "60-90 min", "90-120 min", "Mais de 2 horas"))
        createQuestion(secaoTreino, "Horário habitual de treino", "SINGLE_CHOICE", true, 4,
            options = listOf("Manhã cedo (5h-7h)", "Manhã (7h-10h)", "Final da manhã (10h-12h)", "Almoço (12h-14h)", "Tarde (14h-17h)", "Noite (17h-20h)", "Noite (após 20h)"))
        createQuestion(secaoTreino, "Intensidade média dos treinos", "SCALE", true, 5,
            helpText = "1 = Muito leve, 10 = Máxima intensidade", minValue = 1, maxValue = 10)
        createQuestion(secaoTreino, "Você compete ou pretende competir?", "SINGLE_CHOICE", true, 6,
            options = listOf("Não, treino por hobby/saúde", "Sim, amador", "Sim, profissional", "Pretendo começar a competir"))
        createQuestion(secaoTreino, "Se compete, qual sua próxima competição e quando?", "TEXT_SHORT", false, 7)

        // ----- SEÇÃO 3: OBJETIVOS ESPORTIVOS -----
        val secaoObjetivos = createSection(templateId, "Objetivos Esportivos", "Metas e expectativas", 2)
        
        createQuestion(secaoObjetivos, "Qual seu principal objetivo?", "MULTIPLE_CHOICE", true, 0,
            options = listOf(
                "Hipertrofia (ganho de massa)",
                "Definição/cutting",
                "Perda de gordura",
                "Aumento de força",
                "Melhora de performance",
                "Aumento de resistência",
                "Preparação para competição",
                "Recuperação de lesão",
                "Manutenção",
                "Saúde geral"
            ))
        createQuestion(secaoObjetivos, "Tem alguma meta específica de peso ou composição corporal?", "TEXT_SHORT", false, 1)
        createQuestion(secaoObjetivos, "Tem alguma meta de performance? (Ex: correr 10km em menos de 50min)", "TEXT_LONG", false, 2)

        // ----- SEÇÃO 4: SUPLEMENTAÇÃO ATUAL -----
        val secaoSuplementos = createSection(templateId, "Suplementação", "Suplementos e ergogênicos utilizados", 3)
        
        createQuestion(secaoSuplementos, "Utiliza suplementos atualmente?", "YES_NO", true, 0)
        createQuestion(secaoSuplementos, "Quais suplementos utiliza?", "MULTIPLE_CHOICE", false, 1,
            options = listOf(
                "Whey Protein",
                "Caseína",
                "Albumina",
                "Proteína vegetal",
                "Creatina",
                "BCAA",
                "Glutamina",
                "Beta-alanina",
                "Cafeína/Pré-treino",
                "Carboidrato (maltodextrina, waxy maize, etc)",
                "Omega 3",
                "Vitamina D",
                "Multivitamínico",
                "ZMA",
                "Colágeno",
                "HMB",
                "Outro"
            ))
        createQuestion(secaoSuplementos, "Detalhe a suplementação atual (produto, marca, dosagem, horário):", "TEXT_LONG", false, 2)
        createQuestion(secaoSuplementos, "Já usou ou usa algum recurso ergogênico/hormonal?", "SINGLE_CHOICE", false, 3,
            options = listOf("Nunca usei", "Já usei no passado", "Uso atualmente", "Prefiro não responder"))

        // ----- SEÇÃO 5: ALIMENTAÇÃO E TREINO -----
        val secaoAlimentacaoTreino = createSection(templateId, "Alimentação e Treino", "Hábitos alimentares relacionados ao treino", 4)
        
        createQuestion(secaoAlimentacaoTreino, "Come antes de treinar?", "YES_NO", true, 0)
        createQuestion(secaoAlimentacaoTreino, "O que costuma comer no pré-treino?", "TEXT_LONG", false, 1)
        createQuestion(secaoAlimentacaoTreino, "Quanto tempo antes do treino faz a última refeição?", "SINGLE_CHOICE", false, 2,
            options = listOf("Treino em jejum", "30 min antes", "1 hora antes", "1-2 horas antes", "2-3 horas antes", "Mais de 3 horas"))
        createQuestion(secaoAlimentacaoTreino, "Come/suplementa durante o treino?", "YES_NO", false, 3)
        createQuestion(secaoAlimentacaoTreino, "O que costuma consumir pós-treino?", "TEXT_LONG", false, 4)
        createQuestion(secaoAlimentacaoTreino, "Quanto tempo após o treino faz a refeição pós?", "SINGLE_CHOICE", false, 5,
            options = listOf("Imediatamente", "Até 30 min", "30-60 min", "1-2 horas", "Mais de 2 horas"))
        createQuestion(secaoAlimentacaoTreino, "Quantas refeições faz por dia?", "SINGLE_CHOICE", true, 6,
            options = listOf("3 ou menos", "4", "5", "6", "7 ou mais"))
        createQuestion(secaoAlimentacaoTreino, "Consome proteína em todas as refeições?", "SINGLE_CHOICE", true, 7,
            options = listOf("Sim", "Na maioria", "Às vezes", "Raramente"))

        // ----- SEÇÃO 6: HIDRATAÇÃO -----
        val secaoHidratacao = createSection(templateId, "Hidratação", "Consumo de líquidos", 5)
        
        createQuestion(secaoHidratacao, "Quantos litros de água bebe por dia?", "SINGLE_CHOICE", true, 0,
            options = listOf("Menos de 1L", "1-2L", "2-3L", "3-4L", "Mais de 4L"))
        createQuestion(secaoHidratacao, "Bebe água durante o treino?", "YES_NO", true, 1)
        createQuestion(secaoHidratacao, "Usa algum isotônico ou repositor?", "SINGLE_CHOICE", false, 2,
            options = listOf("Não", "Às vezes", "Frequentemente", "Sempre"))

        // ----- SEÇÃO 7: RECUPERAÇÃO -----
        val secaoRecuperacao = createSection(templateId, "Recuperação", "Sono e recuperação", 6)
        
        createQuestion(secaoRecuperacao, "Quantas horas dorme por noite?", "SINGLE_CHOICE", true, 0,
            options = listOf("Menos de 5h", "5-6h", "6-7h", "7-8h", "Mais de 8h"))
        createQuestion(secaoRecuperacao, "Qualidade do sono", "SCALE", true, 1,
            helpText = "1 = Péssimo, 10 = Excelente", minValue = 1, maxValue = 10)
        createQuestion(secaoRecuperacao, "Sente que se recupera bem entre os treinos?", "SINGLE_CHOICE", true, 2,
            options = listOf("Sim, totalmente", "Na maioria das vezes", "Às vezes", "Raramente", "Não"))
        createQuestion(secaoRecuperacao, "Tem sentido fadiga excessiva ou overtraining?", "YES_NO", false, 3)
        createQuestion(secaoRecuperacao, "Possui alguma lesão atual ou recorrente?", "TEXT_LONG", false, 4)

        // ----- SEÇÃO 8: HISTÓRICO DE SAÚDE -----
        val secaoSaude = createSection(templateId, "Histórico de Saúde", "Condições médicas relevantes", 7)
        
        createQuestion(secaoSaude, "Possui alguma condição de saúde?", "MULTIPLE_CHOICE", true, 0,
            options = listOf("Nenhuma", "Diabetes", "Hipertensão", "Problemas cardíacos", "Problemas articulares", "Problemas de coluna", "Asma", "Anemia", "Outra"))
        createQuestion(secaoSaude, "Usa algum medicamento?", "TEXT_LONG", false, 1)
        createQuestion(secaoSaude, "Tem alguma alergia ou intolerância alimentar?", "TEXT_SHORT", false, 2)
        createQuestion(secaoSaude, "Fez exames recentes? Se sim, houve alguma alteração?", "TEXT_LONG", false, 3)

        println("   ✅ Template 'Anamnese Esportiva' criado")
    }

    // ========================================
    // TEMPLATE 3: RECORDATÓRIO ALIMENTAR 24H
    // ========================================
    private fun createRecordatorio24h() {
        val templateId = AnamnesisTemplates.insert {
            it[professionalId] = SYSTEM_UUID
            it[name] = "Recordatório Alimentar 24h"
            it[description] = "Registro detalhado de tudo que foi consumido nas últimas 24 horas. Ideal para avaliação do padrão alimentar atual."
            it[isDefault] = true
            it[isActive] = true
        }[AnamnesisTemplates.id].value

        // ----- SEÇÃO 1: INFORMAÇÕES DO DIA -----
        val secaoInfo = createSection(templateId, "Informações do Dia", "Contexto do dia avaliado", 0)
        
        createQuestion(secaoInfo, "Data do recordatório", "DATE", true, 0)
        createQuestion(secaoInfo, "Foi um dia típico da sua alimentação?", "YES_NO", true, 1)
        createQuestion(secaoInfo, "Se não, o que foi diferente?", "TEXT_SHORT", false, 2)
        createQuestion(secaoInfo, "Que dia da semana foi?", "SINGLE_CHOICE", true, 3,
            options = listOf("Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo"))
        createQuestion(secaoInfo, "Treinou neste dia?", "YES_NO", false, 4)

        // ----- SEÇÃO 2: CAFÉ DA MANHÃ -----
        val secaoCafe = createSection(templateId, "Café da Manhã", "Primeira refeição do dia", 1)
        
        createQuestion(secaoCafe, "Horário do café da manhã", "TEXT_SHORT", false, 0, placeholder = "Ex: 07:30")
        createQuestion(secaoCafe, "Tomou café da manhã?", "YES_NO", true, 1)
        createQuestion(secaoCafe, "O que consumiu no café da manhã? (Detalhe alimentos e quantidades)", "TEXT_LONG", false, 2,
            placeholder = "Ex: 2 fatias de pão integral, 1 col sopa de requeijão, 1 xícara de café com leite desnatado, 1 banana...")
        createQuestion(secaoCafe, "Local da refeição", "SINGLE_CHOICE", false, 3,
            options = listOf("Casa", "Trabalho", "Padaria/Café", "Carro/Transporte", "Outro"))

        // ----- SEÇÃO 3: LANCHE DA MANHÃ -----
        val secaoLancheManha = createSection(templateId, "Lanche da Manhã", "Refeição intermediária da manhã", 2)
        
        createQuestion(secaoLancheManha, "Fez lanche da manhã?", "YES_NO", true, 0)
        createQuestion(secaoLancheManha, "Horário", "TEXT_SHORT", false, 1, placeholder = "Ex: 10:00")
        createQuestion(secaoLancheManha, "O que consumiu? (Detalhe alimentos e quantidades)", "TEXT_LONG", false, 2)

        // ----- SEÇÃO 4: ALMOÇO -----
        val secaoAlmoco = createSection(templateId, "Almoço", "Refeição principal do meio do dia", 3)
        
        createQuestion(secaoAlmoco, "Horário do almoço", "TEXT_SHORT", false, 0, placeholder = "Ex: 12:30")
        createQuestion(secaoAlmoco, "Almoçou?", "YES_NO", true, 1)
        createQuestion(secaoAlmoco, "O que consumiu no almoço? (Detalhe alimentos e quantidades)", "TEXT_LONG", false, 2,
            placeholder = "Ex: 4 col sopa de arroz, 1 concha de feijão, 1 filé de frango grelhado (120g), salada de alface e tomate à vontade, 1 col sopa de azeite...")
        createQuestion(secaoAlmoco, "Tomou alguma bebida junto?", "TEXT_SHORT", false, 3,
            placeholder = "Ex: 1 copo de suco de laranja natural, refrigerante...")
        createQuestion(secaoAlmoco, "Comeu sobremesa?", "TEXT_SHORT", false, 4)
        createQuestion(secaoAlmoco, "Local da refeição", "SINGLE_CHOICE", false, 5,
            options = listOf("Casa", "Restaurante por quilo", "Restaurante à la carte", "Fast food", "Marmita", "Refeitório", "Outro"))

        // ----- SEÇÃO 5: LANCHE DA TARDE -----
        val secaoLancheTarde = createSection(templateId, "Lanche da Tarde", "Refeição intermediária da tarde", 4)
        
        createQuestion(secaoLancheTarde, "Fez lanche da tarde?", "YES_NO", true, 0)
        createQuestion(secaoLancheTarde, "Horário", "TEXT_SHORT", false, 1, placeholder = "Ex: 16:00")
        createQuestion(secaoLancheTarde, "O que consumiu? (Detalhe alimentos e quantidades)", "TEXT_LONG", false, 2)

        // ----- SEÇÃO 6: JANTAR -----
        val secaoJantar = createSection(templateId, "Jantar", "Refeição principal da noite", 5)
        
        createQuestion(secaoJantar, "Horário do jantar", "TEXT_SHORT", false, 0, placeholder = "Ex: 20:00")
        createQuestion(secaoJantar, "Jantou?", "YES_NO", true, 1)
        createQuestion(secaoJantar, "O que consumiu no jantar? (Detalhe alimentos e quantidades)", "TEXT_LONG", false, 2)
        createQuestion(secaoJantar, "Tomou alguma bebida junto?", "TEXT_SHORT", false, 3)
        createQuestion(secaoJantar, "Comeu sobremesa?", "TEXT_SHORT", false, 4)
        createQuestion(secaoJantar, "Local da refeição", "SINGLE_CHOICE", false, 5,
            options = listOf("Casa", "Restaurante", "Fast food", "Delivery", "Outro"))

        // ----- SEÇÃO 7: CEIA -----
        val secaoCeia = createSection(templateId, "Ceia", "Última refeição do dia", 6)
        
        createQuestion(secaoCeia, "Fez ceia/lanche noturno?", "YES_NO", true, 0)
        createQuestion(secaoCeia, "Horário", "TEXT_SHORT", false, 1, placeholder = "Ex: 22:00")
        createQuestion(secaoCeia, "O que consumiu?", "TEXT_LONG", false, 2)

        // ----- SEÇÃO 8: CONSUMOS EXTRAS -----
        val secaoExtras = createSection(templateId, "Consumos Extras", "Outros alimentos e bebidas ao longo do dia", 7)
        
        createQuestion(secaoExtras, "Consumiu algo entre as refeições não mencionado?", "TEXT_LONG", false, 0,
            placeholder = "Ex: balas, chocolate, biscoitos, cafézinhos...")
        createQuestion(secaoExtras, "Quantos copos/garrafas de água bebeu no dia?", "TEXT_SHORT", true, 1)
        createQuestion(secaoExtras, "Consumiu bebida alcoólica?", "YES_NO", false, 2)
        createQuestion(secaoExtras, "Se sim, o quê e quanto?", "TEXT_SHORT", false, 3)
        createQuestion(secaoExtras, "Tomou suplementos? Quais e quando?", "TEXT_LONG", false, 4)

        // ----- SEÇÃO 9: PERCEPÇÕES -----
        val secaoPercepcoes = createSection(templateId, "Percepções do Dia", "Como você se sentiu", 8)
        
        createQuestion(secaoPercepcoes, "Sentiu fome em algum momento do dia?", "YES_NO", false, 0)
        createQuestion(secaoPercepcoes, "Em qual horário sentiu mais fome?", "TEXT_SHORT", false, 1)
        createQuestion(secaoPercepcoes, "Sentiu algum desconforto após comer? (gases, azia, inchaço...)", "TEXT_LONG", false, 2)
        createQuestion(secaoPercepcoes, "Como estava seu nível de energia durante o dia?", "SCALE", false, 3,
            helpText = "1 = Muito cansado, 10 = Muita energia", minValue = 1, maxValue = 10)

        println("   ✅ Template 'Recordatório Alimentar 24h' criado")
    }

    // ========================================
    // TEMPLATE 4: PRIMEIRA CONSULTA SIMPLIFICADA
    // ========================================
    private fun createAnamnesesPrimeiraConsulta() {
        val templateId = AnamnesisTemplates.insert {
            it[professionalId] = SYSTEM_UUID
            it[name] = "Primeira Consulta - Simplificado"
            it[description] = "Versão resumida para primeira consulta. Rápido de preencher, cobre o essencial."
            it[isDefault] = true
            it[isActive] = true
        }[AnamnesisTemplates.id].value

        // ----- SEÇÃO 1: DADOS BÁSICOS -----
        val secaoDados = createSection(templateId, "Dados Básicos", null, 0)
        
        createQuestion(secaoDados, "Data de nascimento", "DATE", true, 0)
        createQuestion(secaoDados, "Sexo", "SINGLE_CHOICE", true, 1, options = listOf("Masculino", "Feminino"))
        createQuestion(secaoDados, "Profissão", "TEXT_SHORT", true, 2)
        createQuestion(secaoDados, "Peso atual (kg)", "NUMBER", true, 3)
        createQuestion(secaoDados, "Altura (cm)", "NUMBER", true, 4)

        // ----- SEÇÃO 2: OBJETIVO -----
        val secaoObjetivo = createSection(templateId, "Objetivo", null, 1)
        
        createQuestion(secaoObjetivo, "Qual seu principal objetivo?", "SINGLE_CHOICE", true, 0,
            options = listOf("Emagrecer", "Ganhar massa muscular", "Melhorar saúde", "Controle de doença", "Reeducação alimentar", "Outro"))
        createQuestion(secaoObjetivo, "Peso desejado (kg)", "NUMBER", false, 1)

        // ----- SEÇÃO 3: SAÚDE -----
        val secaoSaude = createSection(templateId, "Saúde", null, 2)
        
        createQuestion(secaoSaude, "Possui alguma doença?", "TEXT_LONG", false, 0,
            placeholder = "Ex: Diabetes, hipertensão, colesterol alto...")
        createQuestion(secaoSaude, "Usa algum medicamento?", "TEXT_LONG", false, 1)
        createQuestion(secaoSaude, "Tem alguma alergia ou intolerância alimentar?", "TEXT_SHORT", false, 2)

        // ----- SEÇÃO 4: HÁBITOS -----
        val secaoHabitos = createSection(templateId, "Hábitos", null, 3)
        
        createQuestion(secaoHabitos, "Pratica exercícios?", "YES_NO", true, 0)
        createQuestion(secaoHabitos, "Se sim, quais e com que frequência?", "TEXT_SHORT", false, 1)
        createQuestion(secaoHabitos, "Quantas horas dorme por noite?", "SINGLE_CHOICE", true, 2,
            options = listOf("Menos de 6h", "6-7h", "7-8h", "Mais de 8h"))
        createQuestion(secaoHabitos, "Fuma?", "YES_NO", true, 3)
        createQuestion(secaoHabitos, "Consome bebida alcoólica?", "SINGLE_CHOICE", true, 4,
            options = listOf("Não", "Raramente", "Semanalmente", "Diariamente"))
        createQuestion(secaoHabitos, "Quantos litros de água bebe por dia?", "SINGLE_CHOICE", true, 5,
            options = listOf("Menos de 1L", "1-2L", "2-3L", "Mais de 3L"))

        // ----- SEÇÃO 5: ALIMENTAÇÃO -----
        val secaoAlimentacao = createSection(templateId, "Alimentação", null, 4)
        
        createQuestion(secaoAlimentacao, "Quantas refeições faz por dia?", "SINGLE_CHOICE", true, 0,
            options = listOf("2 ou menos", "3", "4-5", "6 ou mais"))
        createQuestion(secaoAlimentacao, "Quem prepara suas refeições?", "SINGLE_CHOICE", true, 1,
            options = listOf("Eu mesmo", "Familiar", "Compro pronto", "Restaurante"))
        createQuestion(secaoAlimentacao, "Alimentos que não gosta ou não come:", "TEXT_LONG", true, 2)
        createQuestion(secaoAlimentacao, "Tem compulsão ou exagera em algum momento?", "TEXT_SHORT", false, 3)

        println("   ✅ Template 'Primeira Consulta - Simplificado' criado")
    }

    // ========================================
    // TEMPLATE 5: AVALIAÇÃO COMPORTAMENTO ALIMENTAR
    // ========================================
    private fun createAvaliacaoComportamentoAlimentar() {
        val templateId = AnamnesisTemplates.insert {
            it[professionalId] = SYSTEM_UUID
            it[name] = "Avaliação do Comportamento Alimentar"
            it[description] = "Questionário focado em aspectos emocionais e comportamentais da alimentação. Ideal para casos de compulsão, transtornos alimentares ou comer emocional."
            it[isDefault] = true
            it[isActive] = true
        }[AnamnesisTemplates.id].value

        // ----- SEÇÃO 1: RELAÇÃO COM A COMIDA -----
        val secaoRelacao = createSection(templateId, "Relação com a Comida", "Como você se relaciona com a alimentação", 0)
        
        createQuestion(secaoRelacao, "Como você descreveria sua relação com a comida?", "SINGLE_CHOICE", true, 0,
            options = listOf("Muito boa/tranquila", "Boa", "Neutra", "Complicada", "Muito difícil"))
        createQuestion(secaoRelacao, "Você pensa em comida com frequência ao longo do dia?", "SINGLE_CHOICE", true, 1,
            options = listOf("Raramente", "Às vezes", "Frequentemente", "O tempo todo"))
        createQuestion(secaoRelacao, "Sente culpa após comer?", "SINGLE_CHOICE", true, 2,
            options = listOf("Nunca", "Raramente", "Às vezes", "Frequentemente", "Sempre"))
        createQuestion(secaoRelacao, "Já classificou alimentos como 'proibidos'?", "YES_NO", true, 3)
        createQuestion(secaoRelacao, "Quais alimentos considera 'proibidos'?", "TEXT_LONG", false, 4)

        // ----- SEÇÃO 2: SINAIS DE FOME E SACIEDADE -----
        val secaoSinais = createSection(templateId, "Fome e Saciedade", "Percepção dos sinais do corpo", 1)
        
        createQuestion(secaoSinais, "Você consegue identificar quando está com fome?", "SINGLE_CHOICE", true, 0,
            options = listOf("Sim, claramente", "Na maioria das vezes", "Às vezes", "Tenho dificuldade", "Não consigo"))
        createQuestion(secaoSinais, "Você consegue identificar quando está satisfeito?", "SINGLE_CHOICE", true, 1,
            options = listOf("Sim, claramente", "Na maioria das vezes", "Às vezes", "Tenho dificuldade", "Não consigo"))
        createQuestion(secaoSinais, "Costuma comer além do ponto de saciedade?", "SINGLE_CHOICE", true, 2,
            options = listOf("Nunca", "Raramente", "Às vezes", "Frequentemente", "Sempre"))
        createQuestion(secaoSinais, "Come mesmo sem fome física?", "SINGLE_CHOICE", true, 3,
            options = listOf("Nunca", "Raramente", "Às vezes", "Frequentemente", "Sempre"))
        createQuestion(secaoSinais, "Pula refeições propositalmente?", "SINGLE_CHOICE", false, 4,
            options = listOf("Nunca", "Raramente", "Às vezes", "Frequentemente", "Sempre"))

        // ----- SEÇÃO 3: COMER EMOCIONAL -----
        val secaoEmocional = createSection(templateId, "Comer Emocional", "Alimentação ligada às emoções", 2)
        
        createQuestion(secaoEmocional, "Você come quando está estressado?", "SINGLE_CHOICE", true, 0,
            options = listOf("Nunca", "Raramente", "Às vezes", "Frequentemente", "Sempre"))
        createQuestion(secaoEmocional, "Você come quando está triste?", "SINGLE_CHOICE", true, 1,
            options = listOf("Nunca", "Raramente", "Às vezes", "Frequentemente", "Sempre"))
        createQuestion(secaoEmocional, "Você come quando está ansioso?", "SINGLE_CHOICE", true, 2,
            options = listOf("Nunca", "Raramente", "Às vezes", "Frequentemente", "Sempre"))
        createQuestion(secaoEmocional, "Você come quando está entediado?", "SINGLE_CHOICE", true, 3,
            options = listOf("Nunca", "Raramente", "Às vezes", "Frequentemente", "Sempre"))
        createQuestion(secaoEmocional, "Você come para se recompensar ou comemorar?", "SINGLE_CHOICE", true, 4,
            options = listOf("Nunca", "Raramente", "Às vezes", "Frequentemente", "Sempre"))
        createQuestion(secaoEmocional, "A comida é sua principal fonte de conforto?", "YES_NO", true, 5)
        createQuestion(secaoEmocional, "Quais emoções mais te levam a comer?", "MULTIPLE_CHOICE", false, 6,
            options = listOf("Estresse", "Ansiedade", "Tristeza", "Raiva", "Tédio", "Solidão", "Frustração", "Alegria/Comemoração", "Cansaço", "Nenhuma específica"))

        // ----- SEÇÃO 4: COMPULSÃO ALIMENTAR -----
        val secaoCompulsao = createSection(templateId, "Compulsão Alimentar", "Episódios de descontrole", 3)
        
        createQuestion(secaoCompulsao, "Já teve episódios de comer grandes quantidades em pouco tempo?", "SINGLE_CHOICE", true, 0,
            options = listOf("Nunca", "Uma vez", "Raramente", "Às vezes", "Frequentemente", "Muito frequentemente"))
        createQuestion(secaoCompulsao, "Durante esses episódios, sente que perde o controle?", "SINGLE_CHOICE", false, 1,
            options = listOf("Não se aplica", "Não", "Às vezes", "Sim"))
        createQuestion(secaoCompulsao, "Come escondido ou prefere comer sozinho?", "SINGLE_CHOICE", true, 2,
            options = listOf("Nunca", "Raramente", "Às vezes", "Frequentemente", "Sempre"))
        createQuestion(secaoCompulsao, "Após comer muito, já tentou compensar? (vômito, exercício excessivo, jejum)", "YES_NO", true, 3)
        createQuestion(secaoCompulsao, "Com que frequência ocorrem esses episódios de descontrole?", "SINGLE_CHOICE", false, 4,
            options = listOf("Não tenho", "Menos de 1x por mês", "1-3x por mês", "1x por semana", "Várias vezes por semana", "Diariamente"))

        // ----- SEÇÃO 5: IMAGEM CORPORAL -----
        val secaoImagem = createSection(templateId, "Imagem Corporal", "Percepção do próprio corpo", 4)
        
        createQuestion(secaoImagem, "Como você se sente em relação ao seu corpo?", "SCALE", true, 0,
            helpText = "1 = Muito insatisfeito, 10 = Muito satisfeito", minValue = 1, maxValue = 10)
        createQuestion(secaoImagem, "Evita situações por causa do corpo? (praia, fotos, eventos)", "SINGLE_CHOICE", true, 1,
            options = listOf("Nunca", "Raramente", "Às vezes", "Frequentemente", "Sempre"))
        createQuestion(secaoImagem, "Se pesa com frequência?", "SINGLE_CHOICE", true, 2,
            options = listOf("Nunca/Raramente", "1x por semana", "Algumas vezes por semana", "Diariamente", "Várias vezes ao dia"))
        createQuestion(secaoImagem, "O número na balança afeta seu humor ou comportamento alimentar?", "SINGLE_CHOICE", true, 3,
            options = listOf("Não", "Às vezes", "Frequentemente", "Sempre"))

        // ----- SEÇÃO 6: HISTÓRICO -----
        val secaoHistorico = createSection(templateId, "Histórico", "Dietas e tratamentos anteriores", 5)
        
        createQuestion(secaoHistorico, "Com que idade começou a se preocupar com peso/alimentação?", "TEXT_SHORT", false, 0)
        createQuestion(secaoHistorico, "Quantas dietas já fez na vida (aproximadamente)?", "SINGLE_CHOICE", false, 1,
            options = listOf("Nenhuma", "1-2", "3-5", "6-10", "Mais de 10", "Incontáveis"))
        createQuestion(secaoHistorico, "Já fez acompanhamento psicológico?", "YES_NO", false, 2)
        createQuestion(secaoHistorico, "Já foi diagnosticado com algum transtorno alimentar?", "YES_NO", false, 3)
        createQuestion(secaoHistorico, "Se sim, qual?", "TEXT_SHORT", false, 4)
        createQuestion(secaoHistorico, "Há algo mais que gostaria de compartilhar sobre sua relação com a comida?", "TEXT_LONG", false, 5)

        println("   ✅ Template 'Avaliação do Comportamento Alimentar' criado")
    }

    // ========================================
    // FUNÇÕES AUXILIARES
    // ========================================

    private fun createSection(templateId: Int, name: String, description: String?, orderIndex: Int): Int {
        return AnamnesisTemplateSections.insert {
            it[AnamnesisTemplateSections.templateId] = templateId
            it[AnamnesisTemplateSections.name] = name
            it[AnamnesisTemplateSections.description] = description
            it[AnamnesisTemplateSections.orderIndex] = orderIndex
            it[AnamnesisTemplateSections.isActive] = true
        }[AnamnesisTemplateSections.id].value
    }

    private fun createQuestion(
        sectionId: Int,
        question: String,
        questionType: String,
        isRequired: Boolean,
        orderIndex: Int,
        placeholder: String? = null,
        helpText: String? = null,
        minValue: Int? = null,
        maxValue: Int? = null,
        options: List<String>? = null
    ): Int {
        val questionId = AnamnesisTemplateQuestions.insert {
            it[AnamnesisTemplateQuestions.sectionId] = sectionId
            it[AnamnesisTemplateQuestions.question] = question
            it[AnamnesisTemplateQuestions.questionType] = questionType
            it[AnamnesisTemplateQuestions.isRequired] = isRequired
            it[AnamnesisTemplateQuestions.orderIndex] = orderIndex
            it[AnamnesisTemplateQuestions.placeholder] = placeholder
            it[AnamnesisTemplateQuestions.helpText] = helpText
            it[AnamnesisTemplateQuestions.minValue] = minValue
            it[AnamnesisTemplateQuestions.maxValue] = maxValue
            it[AnamnesisTemplateQuestions.isActive] = true
        }[AnamnesisTemplateQuestions.id].value

        // Criar opções se existirem
        options?.forEachIndexed { index, optionText ->
            AnamnesisQuestionOptions.insert {
                it[AnamnesisQuestionOptions.questionId] = questionId
                it[AnamnesisQuestionOptions.optionText] = optionText
                it[AnamnesisQuestionOptions.orderIndex] = index
                it[AnamnesisQuestionOptions.isActive] = true
            }
        }

        return questionId
    }
}