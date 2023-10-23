/*
 * Copyright 2023 Khalid BOURR
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package com.example.B2XKlaim.Service.codeGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Optimizer {
    Generator generator;

    public static List<String> optimize(List<String> code) {
        List<String> optimizedCode = new ArrayList<>();
        String codeString = String.join("", code); // Convert the list to a single string

        // Check if "out()" is followed by "in()" with the same argument
        String[] lines = codeString.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String current = lines[i];
            String next = (i + 1 < lines.length) ? lines[i + 1] : "";

            if (current.startsWith("out(") && next.startsWith("in(")) {
                String currentArg = current.substring(current.indexOf("(") + 1, current.indexOf(")"));
                String nextArg = next.substring(next.indexOf("(") + 1, next.indexOf(")"));

                if (currentArg.equals(nextArg)) {
                    i++;
                    continue; // Skip this and the next line
                }
            }

            optimizedCode.add(current);
        }

        return optimizedCode;
    }
}

