package nl.knaw.huc.hitimepmerger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class Main {

    private final static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main (String args[]) {
        if (args.length < 2 || isBlank(args[0]) || isBlank(args[1])) {
            logger.error("missing arguments");
            logger.info("usage: merger.jar [dump path] [ead path]");
            return;
        }

        Path jsonDump;
        Path eadDir;
        try {
            jsonDump = Path.of(args[0]);
            eadDir = Path.of(args[1]);
        } catch (InvalidPathException e) {
            logger.error("json dump or ead dir does not exist");
            return;
        }
        new MergeService(jsonDump, eadDir, "MERGED").merge();
    }
}
