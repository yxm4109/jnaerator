

# Introduction #

JNAerator is available as a command-line tool, a GUI (JNAerator Studio) and a Maven plugin.

It is however also possible to use it as a library, customizing its behaviour through standard configuration and / or clever subclassing.

# Customization Examples #

## JavaCL Generator ##

JavaCL ships with a Maven plugin that processes OpenCL source code and outputs wrappers that make the OpenCL code visible as Java classes.

[JavaCLGenerator.java](http://code.google.com/p/nativelibs4java/source/browse/trunk/libraries/OpenCL/Generator/src/main/java/com/nativelibs4java/opencl/generator/JavaCLGenerator.java)

The JavaCL generator is used in all JavaCL Demos.

## Another user-contibuted example ##

Contributed by <a href='mailto:romain.rouvoy@lifl.fr'>Romain Rouvoy</a> :
```
import java.io.*;
import java.util.logging.Logger;
import static java.util.logging.Level.*;

import com.ochafik.lang.jnaerator.*;
import com.ochafik.lang.jnaerator.JNAerator.Feedback;

/**
 * JNAerator compiler implementation.
 * 
 * @author <a href="mailto:romain.rouvoy@lifl.fr">Romain Rouvoy</a>
 * @version 1.3
 */
public class JNAeratorCompiler {
    private final JNAeratorConfig cfg = new JNAeratorConfig();
    private final Feedback feedback = new FraSCAtiCompilerFeedback(Logger.getAnonymousLogger());
    private final JNAerator generator = new JNAerator(this.cfg);

    public JNAeratorCompiler() {
        this.cfg.compile = false;
        this.cfg.skipLibraryInstanceDeclarations = true;
        this.cfg.followIncludes = true;
        this.cfg.runtime = JNAeratorConfig.Runtime.JNA;
        this.cfg.autoConf = true;
    }

    public final String interfaceName(File file) {
        String itf = packageName(file);
        return Character.toUpperCase(itf.charAt(0))+itf.substring(1)+"Library";
    }

    public final String packageName(File file) {
        return file.getName().substring(0, file.getName().indexOf('.'));
    }

    public final void compile(File file, File output) throws IOException {
        this.cfg.outputDir = output;
        if (!this.cfg.outputDir.exists())
            this.cfg.outputDir.mkdirs();
        this.cfg.addSourceFile(file, packageName(file), true);
        this.generator.jnaerate(this.feedback);
    }

    private static final class FraSCAtiCompilerFeedback implements Feedback {
        private final Logger logger;

        public FraSCAtiCompilerFeedback(Logger log) {
            this.logger = log;
        }

        /**
         * @see com.ochafik.lang.jnaerator.JNAerator.Feedback#setFinished(java.io.File)
         */
        public void setFinished(File file) {
            if (this.logger.isLoggable(INFO))
                this.logger.info("File '" + file.getName()
                        + "' has been successfully generated.");
        }

        /**
         * @see com.ochafik.lang.jnaerator.JNAerator.Feedback#setFinished(java.lang.Throwable)
         */
        public void setFinished(Throwable error) {
            if (this.logger.isLoggable(SEVERE))
                this.logger.log(SEVERE, "File failed to generate.", error);
        }

        /**
         * @see com.ochafik.lang.jnaerator.JNAerator.Feedback#setStatus(java.lang.String)
         */
        public void setStatus(String status) {
            if (this.logger.isLoggable(FINE))
                this.logger.fine("Status is '" + status + "'");
        }

        /**
         * @see com.ochafik.lang.jnaerator.JNAerator.Feedback#sourcesParsed(com.ochafik.lang.jnaerator.SourceFiles)
         */
        public void sourcesParsed(SourceFiles sources) {
            if (this.logger.isLoggable(FINE))
                this.logger.fine("Parsed sources are: "
                        + sources.getNameSpace());
        }

        /**
         * @see com.ochafik.lang.jnaerator.JNAerator.Feedback#wrappersGenerated(com.ochafik.lang.jnaerator.Result)
         */
        public void wrappersGenerated(Result result) {
            if (this.logger.isLoggable(FINE))
                this.logger.fine("Generated wrappers are: " + result);
        }
    }
}
```