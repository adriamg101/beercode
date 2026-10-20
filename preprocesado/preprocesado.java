package preprocesado;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileWriter;
public class preprocesado {
    public String initialCodePath;
    public static String preprocessedCodePath="preprocesado/processedCode";

    public static void process(String initialCodePath) {
        int character;
        int lastCharacter=1;
        //borro preprocessedCode
        try (FileWriter fw = new FileWriter(preprocessedCodePath)) {
            // Archivo limpio
        } catch (IOException e) {
            //ErrorHnadle!!!!
            return;
        }

        try (FileReader reader = new FileReader(initialCodePath);
             FileWriter writer = new FileWriter(preprocessedCodePath, true)) {

            StringBuilder lineBuffer = new StringBuilder();//eviatr lineas vacias

            while ((character = reader.read()) != -1) {
                // Ver si es un comentario
                if (character == 0xD83C) {
                    character = reader.read();
                    if (character == 0xDF7A) {
                        // Ignorar el resto de la línea
                        while ((character = reader.read()) != -1 && character != '\n') {}
                        writer.write('\n');
                        lineBuffer.setLength(0); // Limpiar  buffer
                        continue;
                    }
                } if (character == '\n') {
                    writer.write('\n');   // Always write the newline
                    lineBuffer.setLength(0); // Still clear the buffer
                } else {
                    // Código normal
                    if (character != '\r' && character != '\t' && !(character == ' ' && lastCharacter == ' ') && !(character == ' ' && lastCharacter == '\n')) {
                        lineBuffer.append((char) character);
                        writer.write(character);
                    }
                }
                lastCharacter = character;
            }
        } catch (IOException e) {
            System.out.println("FILE NOT FOUND... ABORTING!");
        }
    }
}