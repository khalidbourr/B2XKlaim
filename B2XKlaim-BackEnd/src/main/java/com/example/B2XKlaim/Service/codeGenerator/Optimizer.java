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
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Optimizer {

    // Regex to capture argument (group 1) from out(...)@... or in(...)@...
    // Allows for quotes around argument, captures content within quotes.
    private static final Pattern OUT_PATTERN = Pattern.compile("^\\s*out\\((['\"].*?['\"]|[^()]+)\\)@.*");
    private static final Pattern IN_PATTERN = Pattern.compile("^\\s*in\\((['\"].*?['\"]|[^()]+)\\)@.*");

    public static List<String> optimize(List<String> code) {
        if (code == null || code.size() < 2) {
            return new ArrayList<>(code); // Return copy if no optimization possible
        }

        List<String> optimizedCode = new ArrayList<>();
        int i = 0;
        while (i < code.size()) {
            String currentLine = code.get(i);
            String nextLine = (i + 1 < code.size()) ? code.get(i+1) : null;

            String outArg = null;
            String inArg = null;
            boolean matchFound = false;

            if (nextLine != null) {
                Matcher outMatcher = OUT_PATTERN.matcher(currentLine);
                Matcher inMatcher = IN_PATTERN.matcher(nextLine);

                if (outMatcher.matches() && inMatcher.matches()) {
                    outArg = outMatcher.group(1);
                    inArg = inMatcher.group(1);

                    // Check if arguments are non-null and equal
                    if (outArg != null && outArg.equals(inArg)) {
                        log.debug("Optimizer removing lines:\n  {}\n  {}", currentLine, nextLine);
                        i += 2; // Skip both current and next line
                        matchFound = true;
                    }
                }
            }

            if (!matchFound) {
                optimizedCode.add(currentLine); // Add current line if no match found
                i++; // Move to the next line
            }
        }
        return optimizedCode;
    }
}
