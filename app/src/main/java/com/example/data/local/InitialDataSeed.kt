package com.example.data.local

import com.example.data.local.entity.MockTestEntity
import com.example.data.local.entity.QuestionEntity

object InitialDataSeed {

    fun getInitialMockTests(): List<MockTestEntity> = listOf(
        MockTestEntity(
            id = "test_full_01",
            title = "All India Mock Test 01 - GS & CSAT",
            description = "Comprehensive full-length simulation covering Indian Polity, Modern History, Aptitude, Reasoning, and Science.",
            category = "Full Mock Test",
            durationMinutes = 15,
            totalQuestions = 15,
            totalMarks = 30.0,
            passingMarks = 12.0,
            positiveMarksPerQuestion = 2.0,
            negativeMarksPerQuestion = 0.5,
            difficulty = "Moderate",
            subjectsCovered = "Polity, History, Aptitude, Reasoning, Science",
            isAvailableOffline = true
        ),
        MockTestEntity(
            id = "test_polity_02",
            title = "Indian Polity & Constitution Booster",
            description = "High-yield questions on Fundamental Rights, DPSP, Parliament, Constitutional Amendments, and Judiciary.",
            category = "Subject Test",
            durationMinutes = 10,
            totalQuestions = 10,
            totalMarks = 20.0,
            passingMarks = 8.0,
            positiveMarksPerQuestion = 2.0,
            negativeMarksPerQuestion = 0.5,
            difficulty = "Medium",
            subjectsCovered = "Indian Polity, Governance",
            isAvailableOffline = true
        ),
        MockTestEntity(
            id = "test_quant_03",
            title = "Quantitative Aptitude & Reasoning Sprint",
            description = "Speed test covering Percentages, Ratios, Speed & Distance, Coding-Decoding, and Syllogisms.",
            category = "Speed Test",
            durationMinutes = 10,
            totalQuestions = 10,
            totalMarks = 20.0,
            passingMarks = 8.0,
            positiveMarksPerQuestion = 2.0,
            negativeMarksPerQuestion = 0.5,
            difficulty = "Hard",
            subjectsCovered = "Quantitative Aptitude, Logical Reasoning",
            isAvailableOffline = true
        )
    )

    fun getInitialQuestions(): List<QuestionEntity> {
        val list = mutableListOf<QuestionEntity>()

        // Questions for Test 1: Full Mock Test 01 (15 questions)
        list.add(
            QuestionEntity(
                id = "q_f01_1",
                testId = "test_full_01",
                questionNumber = 1,
                subject = "General Studies",
                topic = "Indian Polity",
                questionText = "Which Article of the Indian Constitution guarantees the 'Right to Constitutional Remedies' often termed by Dr. B.R. Ambedkar as the 'Heart and Soul' of the Constitution?",
                optionA = "Article 19",
                optionB = "Article 21",
                optionC = "Article 32",
                optionD = "Article 226",
                correctOptionIndex = 2,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "Article 32 confers the right to move the Supreme Court by appropriate proceedings for the enforcement of the Fundamental Rights. Dr. B.R. Ambedkar famously referred to it as the 'Heart and Soul of the Constitution'. Article 226 empowers High Courts.",
                difficulty = "Easy"
            )
        )
        list.add(
            QuestionEntity(
                id = "q_f01_2",
                testId = "test_full_01",
                questionNumber = 2,
                subject = "General Studies",
                topic = "Modern History",
                questionText = "In which year was the historic Champaran Satyagraha, Mahatma Gandhi's first civil disobedience movement in India, initiated?",
                optionA = "1915",
                optionB = "1917",
                optionC = "1919",
                optionD = "1920",
                correctOptionIndex = 1,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "Mahatma Gandhi organized the Champaran Satyagraha in 1917 in Bihar to protest against the Tinkathia system, where farmers were forced to cultivate Indigo on 3/20th of their land.",
                difficulty = "Medium"
            )
        )
        list.add(
            QuestionEntity(
                id = "q_f01_3",
                testId = "test_full_01",
                questionNumber = 3,
                subject = "Quantitative Aptitude",
                topic = "Percentages",
                questionText = "If the price of a book is first increased by 20% and then decreased by 20%, what is the net percentage change in the price?",
                optionA = "No change (0%)",
                optionB = "4% increase",
                optionC = "4% decrease",
                optionD = "2% decrease",
                correctOptionIndex = 2,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "Formula for successive changes: a + b + (a * b)/100 = 20 + (-20) + (20 * -20)/100 = 0 - 4 = -4%. Hence, there is a net 4% decrease.",
                difficulty = "Easy"
            )
        )
        list.add(
            QuestionEntity(
                id = "q_f01_4",
                testId = "test_full_01",
                questionNumber = 4,
                subject = "Logical Reasoning",
                topic = "Series Completion",
                questionText = "Find the missing number in the sequence: 4, 9, 19, 39, 79, ?",
                optionA = "119",
                optionB = "139",
                optionC = "159",
                optionD = "169",
                correctOptionIndex = 2,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "The pattern is: (Previous number * 2) + 1.\n4 * 2 + 1 = 9\n9 * 2 + 1 = 19\n19 * 2 + 1 = 39\n39 * 2 + 1 = 79\n79 * 2 + 1 = 159.",
                difficulty = "Medium"
            )
        )
        list.add(
            QuestionEntity(
                id = "q_f01_5",
                testId = "test_full_01",
                questionNumber = 5,
                subject = "General Science",
                topic = "Physics",
                questionText = "Which phenomenon explains the blue appearance of the clear sky during daytime?",
                optionA = "Rayleigh Scattering of sunlight by atmospheric molecules",
                optionB = "Total Internal Reflection within water droplets",
                optionC = "Diffraction of light through clouds",
                optionD = "Refraction of light through different thermal layers",
                correctOptionIndex = 0,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "Rayleigh scattering states that the intensity of scattered light is inversely proportional to the 4th power of wavelength (I ∝ 1/λ⁴). Since blue light has a shorter wavelength, it is scattered far more than red light.",
                difficulty = "Medium"
            )
        )
        list.add(
            QuestionEntity(
                id = "q_f01_6",
                testId = "test_full_01",
                questionNumber = 6,
                subject = "General Studies",
                topic = "Geography",
                questionText = "Which Indian river is famously known as the 'Sorrow of Bihar' due to its frequent course shifts and devastating floods?",
                optionA = "Gandak River",
                optionB = "Kosi River",
                optionC = "Son River",
                optionD = "Ghaghara River",
                correctOptionIndex = 1,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "The Kosi River carries heavy silt loads from the Himalayas and frequently alters its channel, causing catastrophic monsoon floods across north Bihar.",
                difficulty = "Easy"
            )
        )
        list.add(
            QuestionEntity(
                id = "q_f01_7",
                testId = "test_full_01",
                questionNumber = 7,
                subject = "Quantitative Aptitude",
                topic = "Time and Work",
                questionText = "A can finish a work in 12 days and B can finish it in 24 days. Working together, in how many days can they complete the same work?",
                optionA = "6 days",
                optionB = "8 days",
                optionC = "10 days",
                optionD = "18 days",
                correctOptionIndex = 1,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "Combined 1-day work = (1/12) + (1/24) = (2 + 1)/24 = 3/24 = 1/8. Therefore, they will complete the entire work in 8 days.",
                difficulty = "Easy"
            )
        )
        list.add(
            QuestionEntity(
                id = "q_f01_8",
                testId = "test_full_01",
                questionNumber = 8,
                subject = "Logical Reasoning",
                topic = "Blood Relations",
                questionText = "Pointing to a photograph of a boy, Suresh says, 'He is the son of the only son of my mother.' How is Suresh related to that boy?",
                optionA = "Brother",
                optionB = "Uncle",
                optionC = "Father",
                optionD = "Cousin",
                correctOptionIndex = 2,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "'Only son of my mother' for a male speaker (Suresh) is Suresh himself. Therefore, the boy is the son of Suresh, making Suresh his father.",
                difficulty = "Medium"
            )
        )
        list.add(
            QuestionEntity(
                id = "q_f01_9",
                testId = "test_full_01",
                questionNumber = 9,
                subject = "General Studies",
                topic = "Indian Economy",
                questionText = "Who is the ex-officio Chairperson of the NITI Aayog in India?",
                optionA = "Finance Minister of India",
                optionB = "President of India",
                optionC = "Prime Minister of India",
                optionD = "Governor of the Reserve Bank of India",
                correctOptionIndex = 2,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "The Prime Minister of India serves as the ex-officio Chairperson of NITI Aayog (National Institution for Transforming India), which replaced the Planning Commission in 2015.",
                difficulty = "Easy"
            )
        )
        list.add(
            QuestionEntity(
                id = "q_f01_10",
                testId = "test_full_01",
                questionNumber = 10,
                subject = "General Science",
                topic = "Biology",
                questionText = "Which cellular organelle is known as the 'Powerhouse of the Cell' due to ATP production?",
                optionA = "Ribosome",
                optionB = "Golgi Apparatus",
                optionC = "Mitochondria",
                optionD = "Endoplasmic Reticulum",
                correctOptionIndex = 2,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "Mitochondria produce ATP (Adenosine Triphosphate) through cellular respiration and oxidative phosphorylation, earning the title 'Powerhouse of the Cell'.",
                difficulty = "Easy"
            )
        )
        list.add(
            QuestionEntity(
                id = "q_f01_11",
                testId = "test_full_01",
                questionNumber = 11,
                subject = "Quantitative Aptitude",
                topic = "Simple & Compound Interest",
                questionText = "What is the simple interest on a principal sum of ₹5,000 at an annual interest rate of 6% over 3 years?",
                optionA = "₹750",
                optionB = "₹900",
                optionC = "₹1,050",
                optionD = "₹1,200",
                correctOptionIndex = 1,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "Simple Interest = (P * R * T) / 100 = (5000 * 6 * 3) / 100 = 50 * 18 = ₹900.",
                difficulty = "Easy"
            )
        )
        list.add(
            QuestionEntity(
                id = "q_f01_12",
                testId = "test_full_01",
                questionNumber = 12,
                subject = "Logical Reasoning",
                topic = "Direction Sense",
                questionText = "A person walks 10 km North, turns right and walks 6 km, then turns right again and walks 10 km. In which direction is he from the starting point?",
                optionA = "North",
                optionB = "South",
                optionC = "East",
                optionD = "West",
                correctOptionIndex = 2,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "Walking 10 km North, turning right (East) 6 km, then right (South) 10 km cancels out the vertical displacement. He is exactly 6 km East of the starting point.",
                difficulty = "Medium"
            )
        )
        list.add(
            QuestionEntity(
                id = "q_f01_13",
                testId = "test_full_01",
                questionNumber = 13,
                subject = "General Studies",
                topic = "Environment & Ecology",
                questionText = "Which International Protocol signed in 1987 is dedicated to phasing out substances that deplete the Ozone Layer?",
                optionA = "Kyoto Protocol",
                optionB = "Montreal Protocol",
                optionC = "Paris Agreement",
                optionD = "Cartagena Protocol",
                correctOptionIndex = 1,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "The Montreal Protocol on Substances that Deplete the Ozone Layer was finalized in 1987. It phased out chlorofluorocarbons (CFCs) and is regarded as one of the most successful environmental treaties.",
                difficulty = "Medium"
            )
        )
        list.add(
            QuestionEntity(
                id = "q_f01_14",
                testId = "test_full_01",
                questionNumber = 14,
                subject = "Quantitative Aptitude",
                topic = "Averages",
                questionText = "The average of 5 consecutive odd numbers is 27. What is the value of the largest number?",
                optionA = "29",
                optionB = "31",
                optionC = "33",
                optionD = "35",
                correctOptionIndex = 1,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "For 5 consecutive odd numbers, the middle number (3rd number) is the average, which is 27. The numbers are 23, 25, 27, 29, 31. The largest number is 31.",
                difficulty = "Easy"
            )
        )
        list.add(
            QuestionEntity(
                id = "q_f01_15",
                testId = "test_full_01",
                questionNumber = 15,
                subject = "General Studies",
                topic = "Indian Polity",
                questionText = "Which Constitutional Amendment Act reduced the voting age in India from 21 years to 18 years for Lok Sabha and Legislative Assembly elections?",
                optionA = "42nd Amendment Act",
                optionB = "44th Amendment Act",
                optionC = "61st Amendment Act",
                optionD = "73rd Amendment Act",
                correctOptionIndex = 2,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "The 61st Constitutional Amendment Act, 1988 (which came into force in 1989) amended Article 326 to lower the voting age from 21 to 18 years.",
                difficulty = "Medium"
            )
        )

        // Questions for Test 2: Indian Polity & Constitution Booster (10 questions)
        val polityQuestions = listOf(
            QuestionEntity(
                id = "q_pol_1",
                testId = "test_polity_02",
                questionNumber = 1,
                subject = "General Studies",
                topic = "Indian Polity",
                questionText = "Which schedule of the Indian Constitution contains provisions regarding the disqualification of members on the ground of defection (Anti-Defection Law)?",
                optionA = "7th Schedule",
                optionB = "8th Schedule",
                optionC = "10th Schedule",
                optionD = "11th Schedule",
                correctOptionIndex = 2,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "The 10th Schedule was added by the 52nd Constitutional Amendment Act, 1985, formulating the Anti-Defection Law.",
                difficulty = "Easy"
            ),
            QuestionEntity(
                id = "q_pol_2",
                testId = "test_polity_02",
                questionNumber = 2,
                subject = "General Studies",
                topic = "Indian Polity",
                questionText = "Under which Article of the Constitution can the President of India declare a National Emergency on grounds of war, external aggression, or armed rebellion?",
                optionA = "Article 352",
                optionB = "Article 356",
                optionC = "Article 360",
                optionD = "Article 368",
                correctOptionIndex = 0,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "Article 352 deals with National Emergency. Article 356 relates to President's Rule (State Emergency) and Article 360 relates to Financial Emergency.",
                difficulty = "Medium"
            ),
            QuestionEntity(
                id = "q_pol_3",
                testId = "test_polity_02",
                questionNumber = 3,
                subject = "General Studies",
                topic = "Indian Polity",
                questionText = "Who presides over a joint sitting of both Houses of Parliament in India?",
                optionA = "President of India",
                optionB = "Vice-President of India",
                optionC = "Speaker of Lok Sabha",
                optionD = "Prime Minister of India",
                correctOptionIndex = 2,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "Under Article 118(4), the Speaker of the Lok Sabha presides over a joint sitting of both Houses. In their absence, the Deputy Speaker presides.",
                difficulty = "Medium"
            ),
            QuestionEntity(
                id = "q_pol_4",
                testId = "test_polity_02",
                questionNumber = 4,
                subject = "General Studies",
                topic = "Indian Polity",
                questionText = "Which writ literally translates to 'We Command' and is issued to enforce the performance of a public duty?",
                optionA = "Habeas Corpus",
                optionB = "Mandamus",
                optionC = "Certiorari",
                optionD = "Quo Warranto",
                correctOptionIndex = 1,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "Mandamus is a Latin word meaning 'We Command'. It is issued by the court to a public official asking them to perform duties they have failed or refused to perform.",
                difficulty = "Medium"
            ),
            QuestionEntity(
                id = "q_pol_5",
                testId = "test_polity_02",
                questionNumber = 5,
                subject = "General Studies",
                topic = "Indian Polity",
                questionText = "The Directive Principles of State Policy (DPSP) in Part IV of the Indian Constitution were inspired by the Constitution of which country?",
                optionA = "United States of America",
                optionB = "Ireland",
                optionC = "United Kingdom",
                optionD = "Australia",
                correctOptionIndex = 1,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "The framers of the Indian Constitution borrowed the concept of Directive Principles of State Policy from the Irish Constitution of 1937.",
                difficulty = "Easy"
            ),
            QuestionEntity(
                id = "q_pol_6",
                testId = "test_polity_02",
                questionNumber = 6,
                subject = "General Studies",
                topic = "Indian Polity",
                questionText = "What is the minimum age required for a citizen to be eligible for election as the President of India?",
                optionA = "25 years",
                optionB = "30 years",
                optionC = "35 years",
                optionD = "40 years",
                correctOptionIndex = 2,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "Under Article 58, a candidate must have completed 35 years of age to be eligible for election as the President of India.",
                difficulty = "Easy"
            ),
            QuestionEntity(
                id = "q_pol_7",
                testId = "test_polity_02",
                questionNumber = 7,
                subject = "General Studies",
                topic = "Indian Polity",
                questionText = "By which Constitutional Amendment were the words 'Socialist, Secular, and Integrity' added to the Preamble?",
                optionA = "24th Amendment Act",
                optionB = "42nd Amendment Act",
                optionC = "44th Amendment Act",
                optionD = "52nd Amendment Act",
                correctOptionIndex = 1,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "The 42nd Constitutional Amendment Act of 1976 amended the Preamble by adding the three words 'Socialist', 'Secular', and 'Integrity'.",
                difficulty = "Medium"
            ),
            QuestionEntity(
                id = "q_pol_8",
                testId = "test_polity_02",
                questionNumber = 8,
                subject = "General Studies",
                topic = "Indian Polity",
                questionText = "How many members are nominated by the President of India to the Rajya Sabha for special knowledge in literature, science, art, or social service?",
                optionA = "2",
                optionB = "10",
                optionC = "12",
                optionD = "14",
                correctOptionIndex = 2,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "Under Article 80 of the Constitution, the President nominates 12 members to the Rajya Sabha having special knowledge or practical experience in literature, science, art, and social service.",
                difficulty = "Easy"
            ),
            QuestionEntity(
                id = "q_pol_9",
                testId = "test_polity_02",
                questionNumber = 9,
                subject = "General Studies",
                topic = "Indian Polity",
                questionText = "The concept of 'Basic Structure' of the Indian Constitution was propounded by the Supreme Court in which landmark case?",
                optionA = "Golaknath case (1967)",
                optionB = "Kesavananda Bharati case (1973)",
                optionC = "Minerva Mills case (1980)",
                optionD = "Maneka Gandhi case (1978)",
                correctOptionIndex = 1,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "In the Kesavananda Bharati v. State of Kerala (1973) case, a 13-judge bench established the Basic Structure doctrine, ruling that Parliament cannot alter the fundamental features of the Constitution.",
                difficulty = "Hard"
            ),
            QuestionEntity(
                id = "q_pol_10",
                testId = "test_polity_02",
                questionNumber = 10,
                subject = "General Studies",
                topic = "Indian Polity",
                questionText = "Which Article of the Constitution provides for the establishment of the Finance Commission every five years?",
                optionA = "Article 280",
                optionB = "Article 300A",
                optionC = "Article 324",
                optionD = "Article 315",
                correctOptionIndex = 0,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "Article 280 provides for the constitution of a Finance Commission by the President of India at the expiration of every fifth year to recommend distribution of tax proceeds.",
                difficulty = "Medium"
            )
        )
        list.addAll(polityQuestions)

        // Questions for Test 3: Quant & Reasoning Sprint (10 questions)
        val quantQuestions = listOf(
            QuestionEntity(
                id = "q_qr_1",
                testId = "test_quant_03",
                questionNumber = 1,
                subject = "Quantitative Aptitude",
                topic = "Speed, Time & Distance",
                questionText = "A train 150 meters long crosses a pole in 9 seconds. What is the speed of the train in km/h?",
                optionA = "50 km/h",
                optionB = "60 km/h",
                optionC = "72 km/h",
                optionD = "80 km/h",
                correctOptionIndex = 1,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "Speed in m/s = Distance / Time = 150 / 9 = 50/3 m/s.\nConvert to km/h: (50/3) * (18/5) = 10 * 6 = 60 km/h.",
                difficulty = "Medium"
            ),
            QuestionEntity(
                id = "q_qr_2",
                testId = "test_quant_03",
                questionNumber = 2,
                subject = "Quantitative Aptitude",
                topic = "Ratio & Proportion",
                questionText = "If A : B = 3 : 4 and B : C = 8 : 9, what is the ratio A : C?",
                optionA = "1 : 2",
                optionB = "2 : 3",
                optionC = "3 : 4",
                optionD = "1 : 3",
                correctOptionIndex = 1,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "A/C = (A/B) * (B/C) = (3/4) * (8/9) = (3 * 8) / (4 * 9) = 24 / 36 = 2/3. So A : C = 2 : 3.",
                difficulty = "Easy"
            ),
            QuestionEntity(
                id = "q_qr_3",
                testId = "test_quant_03",
                questionNumber = 3,
                subject = "Logical Reasoning",
                topic = "Coding-Decoding",
                questionText = "If in a certain code language 'TEACHER' is coded as 'VGCEJGT', how will 'STUDENT' be coded in that language?",
                optionA = "UVWFGPV",
                optionB = "UVVFFOV",
                optionC = "UVWFGOV",
                optionD = "TUVEFOV",
                correctOptionIndex = 0,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "Each letter is shifted forward by +2:\nS(+2)->U, T(+2)->V, U(+2)->W, D(+2)->F, E(+2)->G, N(+2)->P, T(+2)->V => 'UVWFGPV'.",
                difficulty = "Easy"
            ),
            QuestionEntity(
                id = "q_qr_4",
                testId = "test_quant_03",
                questionNumber = 4,
                subject = "Quantitative Aptitude",
                topic = "Profit and Loss",
                questionText = "A shopkeeper sells an article for ₹840 gaining a 20% profit. What was the original cost price of the article?",
                optionA = "₹680",
                optionB = "₹700",
                optionC = "₹720",
                optionD = "₹750",
                correctOptionIndex = 1,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "Selling Price = Cost Price * (100 + Profit%)/100 => 840 = CP * (120/100) => CP = (840 * 100) / 120 = 7 * 100 = ₹700.",
                difficulty = "Easy"
            ),
            QuestionEntity(
                id = "q_qr_5",
                testId = "test_quant_03",
                questionNumber = 5,
                subject = "Logical Reasoning",
                topic = "Syllogism",
                questionText = "Statements:\n1. All roses are flowers.\n2. Some flowers are red.\nConclusions:\nI. Some roses are red.\nII. All flowers are roses.",
                optionA = "Only Conclusion I follows",
                optionB = "Only Conclusion II follows",
                optionC = "Neither Conclusion I nor II follows",
                optionD = "Both Conclusions I and II follow",
                correctOptionIndex = 2,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "The overlap between roses and flowers does not guarantee that the red flowers include roses. Also, all roses are flowers does not imply all flowers are roses. Hence neither follows.",
                difficulty = "Hard"
            ),
            QuestionEntity(
                id = "q_qr_6",
                testId = "test_quant_03",
                questionNumber = 6,
                subject = "Quantitative Aptitude",
                topic = "Pipes & Cisterns",
                questionText = "Pipe A can fill a tank in 6 hours and Pipe B can empty it in 8 hours. If both are opened simultaneously, how long will it take to fill the tank?",
                optionA = "14 hours",
                optionB = "20 hours",
                optionC = "24 hours",
                optionD = "48 hours",
                correctOptionIndex = 2,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "Net 1-hour filling rate = (1/6) - (1/8) = (4 - 3)/24 = 1/24 tank per hour. Therefore, the tank will be filled in 24 hours.",
                difficulty = "Medium"
            ),
            QuestionEntity(
                id = "q_qr_7",
                testId = "test_quant_03",
                questionNumber = 7,
                subject = "Logical Reasoning",
                topic = "Number Analogy",
                questionText = "Select the related number from the given alternatives: 12 : 144 :: 15 : ?",
                optionA = "215",
                optionB = "225",
                optionC = "235",
                optionD = "255",
                correctOptionIndex = 1,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "The pattern is squaring the number: 12² = 144, so 15² = 225.",
                difficulty = "Easy"
            ),
            QuestionEntity(
                id = "q_qr_8",
                testId = "test_quant_03",
                questionNumber = 8,
                subject = "Quantitative Aptitude",
                topic = "Mixtures & Alligation",
                questionText = "In what ratio must tea worth ₹60 per kg be mixed with tea worth ₹65 per kg so that the mixture is worth ₹62 per kg?",
                optionA = "2 : 3",
                optionB = "3 : 2",
                optionC = "3 : 5",
                optionD = "5 : 3",
                correctOptionIndex = 1,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "By Rule of Alligation:\n(Cheaper Price: 60) and (Dearer Price: 65), Mean Price: 62.\nRatio = (65 - 62) : (62 - 60) = 3 : 2.",
                difficulty = "Medium"
            ),
            QuestionEntity(
                id = "q_qr_9",
                testId = "test_quant_03",
                questionNumber = 9,
                subject = "Logical Reasoning",
                topic = "Seating Arrangement",
                questionText = "Five friends P, Q, R, S, and T are standing in a row facing North. R is to the immediate right of P. Q is between S and T. T is at the left extreme. Who is sitting in the middle?",
                optionA = "P",
                optionB = "Q",
                optionC = "R",
                optionD = "S",
                correctOptionIndex = 3,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "Left extreme is T. Since Q is between S and T, order is T - Q - S. Since R is to immediate right of P, they must occupy the remaining two spots: P - R. Full order: T, Q, S, P, R. The middle person is S.",
                difficulty = "Hard"
            ),
            QuestionEntity(
                id = "q_qr_10",
                testId = "test_quant_03",
                questionNumber = 10,
                subject = "Quantitative Aptitude",
                topic = "Algebra",
                questionText = "If x + (1/x) = 4, find the value of x² + (1/x²).",
                optionA = "12",
                optionB = "14",
                optionC = "16",
                optionD = "18",
                correctOptionIndex = 1,
                marks = 2.0,
                negativeMarks = 0.5,
                explanation = "Squaring both sides: (x + 1/x)² = 4² => x² + 2 + 1/x² = 16 => x² + 1/x² = 16 - 2 = 14.",
                difficulty = "Medium"
            )
        )
        list.addAll(quantQuestions)

        return list
    }
}
