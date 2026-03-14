-- ==========================================
-- Módulo: Montagem de Treinos (Personal)
-- Tabelas para o sistema de treinos
-- ==========================================

-- Módulos da Biblioteca do Personal (Templates reutilizáveis)
CREATE TABLE IF NOT EXISTS workout_modules (
    id SERIAL PRIMARY KEY,
    professional_id INT NOT NULL REFERENCES professionals(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    estimated_duration_minutes INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_workout_modules_professional ON workout_modules(professional_id);

-- Exercícios dentro de um Módulo da Biblioteca
CREATE TABLE IF NOT EXISTS workout_module_exercises (
    id SERIAL PRIMARY KEY,
    module_id INT NOT NULL REFERENCES workout_modules(id) ON DELETE CASCADE,
    exercise_name VARCHAR(100) NOT NULL,
    exercise_gif_url VARCHAR(255),
    muscle_group VARCHAR(50),
    sets INT NOT NULL,
    reps VARCHAR(20) NOT NULL,
    rest_seconds INT,
    technique VARCHAR(100),
    notes TEXT,
    order_index INT NOT NULL
);

-- Treinos Vinculados aos Alunos (Staging/Publicados)
CREATE TABLE IF NOT EXISTS student_workouts (
    id SERIAL PRIMARY KEY,
    professional_id INT NOT NULL REFERENCES professionals(id) ON DELETE CASCADE,
    student_id INT NOT NULL REFERENCES professional_students(id) ON DELETE CASCADE,
    module_id INT REFERENCES workout_modules(id) ON DELETE SET NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    day_of_week VARCHAR(10),
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_student_workouts_professional ON student_workouts(professional_id);
CREATE INDEX IF NOT EXISTS idx_student_workouts_student ON student_workouts(student_id);
CREATE INDEX IF NOT EXISTS idx_student_workouts_status ON student_workouts(status);

-- Exercícios do Treino do Aluno (Cópia independente)
CREATE TABLE IF NOT EXISTS student_workout_exercises (
    id SERIAL PRIMARY KEY,
    student_workout_id INT NOT NULL REFERENCES student_workouts(id) ON DELETE CASCADE,
    exercise_name VARCHAR(100) NOT NULL,
    exercise_gif_url VARCHAR(255),
    muscle_group VARCHAR(50),
    sets INT NOT NULL,
    reps VARCHAR(20) NOT NULL,
    weight VARCHAR(20),
    rest_seconds INT,
    technique VARCHAR(100),
    notes TEXT,
    order_index INT NOT NULL
);
