package co.onmind.hex4w.transverse.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

/**
 * Aspect for reactive method logging and performance monitoring.
 * 
 * This aspect provides automatic logging capabilities for reactive methods,
 * including execution time monitoring, parameter logging, and result logging.
 * It's specifically designed to work with reactive types (Mono and Flux)
 * without blocking the reactive streams.
 * 
 * <p>Features provided:</p>
 * <ul>
 *   <li>Automatic method entry/exit logging</li>
 *   <li>Parameter and return value logging</li>
 *   <li>Execution time measurement</li>
 *   <li>Error logging with stack traces</li>
 *   <li>Reactive stream-aware logging</li>
 * </ul>
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 * @since 1.0.0
 */
@Aspect
@Component
public class ReactiveLoggingAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(ReactiveLoggingAspect.class);
    
    /**
     * Logs execution of service layer methods.
     * 
     * This advice intercepts all methods in service packages and provides
     * comprehensive logging including timing, parameters, and results.
     * It handles both reactive and non-reactive return types appropriately.
     * 
     * @param joinPoint the method execution join point
     * @return the method result (potentially wrapped for reactive types)
     * @throws Throwable if the method execution fails
     */
    @Around("execution(* co.onmind.hex4jwebflux.domain.services..*(..))")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint, "SERVICE");
    }
    
    /**
     * Logs execution of use case methods.
     * 
     * This advice intercepts all methods in use case packages and provides
     * comprehensive logging for application layer operations.
     * 
     * @param joinPoint the method execution join point
     * @return the method result (potentially wrapped for reactive types)
     * @throws Throwable if the method execution fails
     */
    @Around("execution(* co.onmind.hex4jwebflux.application.usecases..*(..))")
    public Object logUseCaseMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint, "USE_CASE");
    }
    
    /**
     * Logs execution of repository adapter methods.
     * 
     * This advice intercepts all methods in repository adapter packages
     * and provides logging for data access operations.
     * 
     * @param joinPoint the method execution join point
     * @return the method result (potentially wrapped for reactive types)
     * @throws Throwable if the method execution fails
     */
    @Around("execution(* co.onmind.hex4jwebflux.infrastructure.persistence.adapters..*(..))")
    public Object logRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint, "REPOSITORY");
    }
    
    /**
     * Core method execution logging logic.
     * 
     * This method handles the actual logging logic for method execution,
     * including timing, parameter logging, and reactive stream handling.
     * 
     * @param joinPoint the method execution join point
     * @param layer the application layer name for logging context
     * @return the method result (potentially wrapped for reactive types)
     * @throws Throwable if the method execution fails
     */
    private Object logMethodExecution(ProceedingJoinPoint joinPoint, String layer) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        Instant startTime = Instant.now();
        
        // Log method entry
        logMethodEntry(className, methodName, args, layer);
        
        try {
            Object result = joinPoint.proceed();
            
            // Handle reactive return types
            if (result instanceof Mono<?> mono) {
                return mono
                    .doOnSuccess(value -> logMethodSuccess(className, methodName, value, startTime, layer))
                    .doOnError(error -> logMethodError(className, methodName, error, startTime, layer));
            } else if (result instanceof Flux<?> flux) {
                return flux
                    .doOnComplete(() -> logMethodSuccess(className, methodName, "Flux completed", startTime, layer))
                    .doOnError(error -> logMethodError(className, methodName, error, startTime, layer));
            } else {
                // Handle non-reactive return types
                logMethodSuccess(className, methodName, result, startTime, layer);
                return result;
            }
            
        } catch (Throwable throwable) {
            logMethodError(className, methodName, throwable, startTime, layer);
            throw throwable;
        }
    }
    
    /**
     * Logs method entry with parameters.
     * 
     * @param className the class name
     * @param methodName the method name
     * @param args the method arguments
     * @param layer the application layer
     */
    private void logMethodEntry(String className, String methodName, Object[] args, String layer) {
        if (logger.isDebugEnabled()) {
            logger.debug("[{}] Entering method: {}.{}() with args: {}", 
                layer, className, methodName, formatArgs(args));
        } else {
            logger.info("[{}] Executing: {}.{}()", layer, className, methodName);
        }
    }
    
    /**
     * Logs successful method completion.
     * 
     * @param className the class name
     * @param methodName the method name
     * @param result the method result
     * @param startTime the method start time
     * @param layer the application layer
     */
    private void logMethodSuccess(String className, String methodName, Object result, 
                                Instant startTime, String layer) {
        Duration duration = Duration.between(startTime, Instant.now());
        
        if (logger.isDebugEnabled()) {
            logger.debug("[{}] Method completed: {}.{}() in {}ms - Result: {}", 
                layer, className, methodName, duration.toMillis(), formatResult(result));
        } else {
            logger.info("[{}] Completed: {}.{}() in {}ms", 
                layer, className, methodName, duration.toMillis());
        }
        
        // Log performance warning for slow operations
        if (duration.toMillis() > 500) {
            logger.warn("[{}] Slow operation detected: {}.{}() took {}ms", 
                layer, className, methodName, duration.toMillis());
        }
    }
    
    /**
     * Logs method execution errors.
     * 
     * @param className the class name
     * @param methodName the method name
     * @param error the error that occurred
     * @param startTime the method start time
     * @param layer the application layer
     */
    private void logMethodError(String className, String methodName, Throwable error, 
                              Instant startTime, String layer) {
        Duration duration = Duration.between(startTime, Instant.now());
        
        logger.error("[{}] Method failed: {}.{}() in {}ms - Error: {} - Message: {}", 
            layer, className, methodName, duration.toMillis(), 
            error.getClass().getSimpleName(), error.getMessage());
        
        // Log stack trace for unexpected errors
        if (!(error instanceof IllegalArgumentException) && 
            !error.getClass().getPackage().getName().contains("co.onmind.hex4jwebflux.domain.exceptions")) {
            logger.error("Stack trace:", error);
        }
    }
    
    /**
     * Formats method arguments for logging.
     * 
     * @param args the method arguments
     * @return formatted argument string
     */
    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        
        return Arrays.stream(args)
            .map(this::formatArgument)
            .reduce((a, b) -> a + ", " + b)
            .map(s -> "[" + s + "]")
            .orElse("[]");
    }
    
    /**
     * Formats a single argument for logging.
     * 
     * @param arg the argument to format
     * @return formatted argument string
     */
    private String formatArgument(Object arg) {
        if (arg == null) {
            return "null";
        }
        
        String argString = arg.toString();
        // Truncate long strings
        if (argString.length() > 100) {
            return argString.substring(0, 97) + "...";
        }
        
        return argString;
    }
    
    /**
     * Formats method result for logging.
     * 
     * @param result the method result
     * @return formatted result string
     */
    private String formatResult(Object result) {
        if (result == null) {
            return "null";
        }
        
        String resultString = result.toString();
        // Truncate long results
        if (resultString.length() > 200) {
            return resultString.substring(0, 197) + "...";
        }
        
        return resultString;
    }
}