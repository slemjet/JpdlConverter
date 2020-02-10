package com.slemjet.jpdlconverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(JpdlToUmlConverter.class);

    public static void main(String[] args) {

        String pathStr = "F:\\IDEA_Projects\\JpdlConverter\\src\\main\\java\\com\\slemjet\\jpdlconverter\\jpdl\\sample.jpdl";
        File file = new File(pathStr);

        if(!file.exists()){
            logger.error(String.format("File does not exist: %s", pathStr));
            return;
        }

        JpdlToUmlConverter converter = new JpdlToUmlConverter();
        converter.convertToUml(file);
    }
}
