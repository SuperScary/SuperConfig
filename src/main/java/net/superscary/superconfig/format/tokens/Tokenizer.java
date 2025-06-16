package net.superscary.superconfig.format.tokens;

import java.io.IOException;
import java.util.List;

public interface Tokenizer {
    List<Token> tokenize() throws IOException;
} 