/*
 * Copyright 2010 JBoss Inc
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

package org.drools;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.drools.command.CommandService;
import org.drools.core.util.ConfFileUtils;
import org.drools.core.util.StringUtils;
import org.drools.marshalling.ObjectMarshallingStrategy;
import org.drools.marshalling.impl.ClassObjectMarshallingStrategyAcceptor;
import org.drools.marshalling.impl.SerializablePlaceholderResolverStrategy;
import org.drools.process.instance.WorkItemManagerFactory;
import org.drools.runtime.Environment;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.conf.ClockTypeOption;
import org.drools.runtime.conf.KeepReferenceOption;
import org.drools.runtime.conf.KnowledgeSessionOption;
import org.drools.runtime.conf.MultiValueKnowledgeSessionOption;
import org.drools.runtime.conf.QueryListenerOption;
import org.drools.runtime.conf.SingleValueKnowledgeSessionOption;
import org.drools.runtime.conf.WorkItemHandlerOption;
import org.drools.runtime.process.WorkItemHandler;
import org.drools.time.TimerService;
import org.drools.util.ChainedProperties;
import org.drools.util.ClassLoaderUtil;
import org.drools.util.CompositeClassLoader;
import org.mvel2.MVEL;

/**
 * SessionConfiguration
 *
 * A class to store Session related configuration. It must be used at session instantiation time
 * or not used at all.
 * This class will automatically load default values from system properties, so if you want to set
 * a default configuration value for all your new sessions, you can simply set the property as
 * a System property.
 *
 * After the Session is created, it makes the configuration immutable and there is no way to make it
 * mutable again. This is to avoid inconsistent behavior inside session.
 *
 * NOTE: This API is under review and may change in the future.
 * 
 * 
 * drools.keepReference = <true|false>
 * drools.clockType = <pseudo|realtime|heartbeat|implicit>
 */
public class SessionConfiguration
    implements
    KnowledgeSessionConfiguration,
    Externalizable {
    private static final long              serialVersionUID = 510l;

    private ChainedProperties              chainedProperties;

    private volatile boolean               immutable;

    private boolean                        keepReference;

    private ClockType                      clockType;

    private QueryListenerOption            queryListener;

    private Map<String, WorkItemHandler>   workItemHandlers;
    private WorkItemManagerFactory         workItemManagerFactory;
    private CommandService                 commandService;

    private transient CompositeClassLoader classLoader;

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject( chainedProperties );
        out.writeBoolean( immutable );
        out.writeBoolean( keepReference );
        out.writeObject( clockType );
        out.writeObject( queryListener );
    }
    
    private static final SessionConfiguration defaultInstance = new SessionConfiguration();
    
    public static SessionConfiguration getDefaultInstance() {
        return defaultInstance;
    }

    @SuppressWarnings("unchecked")
    public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {
        chainedProperties = (ChainedProperties) in.readObject();
        immutable = in.readBoolean();
        keepReference = in.readBoolean();
        clockType = (ClockType) in.readObject();
        queryListener = (QueryListenerOption) in.readObject();
    }

    /**
     * Creates a new session configuration using the provided properties
     * as configuration options. 
     *
     * @param properties
     */
    public SessionConfiguration(Properties properties) {
        init( properties,
              null );
    }

    /**
     * Creates a new session configuration with default configuration options.
     */
    public SessionConfiguration() {
        init( null,
              null );
    }

    public SessionConfiguration(ClassLoader... classLoader) {
        init( null,
              classLoader );
    }

    private void init(Properties properties,
                      ClassLoader... classLoader) {
        this.classLoader = ClassLoaderUtil.getClassLoader( classLoader,
                                                           getClass(),
                                                           false );

        this.immutable = false;
        this.chainedProperties = new ChainedProperties( "session.conf",
                                                        this.classLoader );

        if ( properties != null ) {
            this.chainedProperties.addProperties( properties );
        }

        setKeepReference( Boolean.valueOf( this.chainedProperties.getProperty( KeepReferenceOption.PROPERTY_NAME,
                                                                               "true" ) ).booleanValue() );

        setClockType( ClockType.resolveClockType( this.chainedProperties.getProperty( ClockTypeOption.PROPERTY_NAME,
                                                                                      ClockType.REALTIME_CLOCK.getId() ) ) );

        setQueryListenerClass( this.chainedProperties.getProperty( QueryListenerOption.PROPERTY_NAME,
                                                                   QueryListenerOption.STANDARD.getAsString() ) );
    }

    public void addProperties(Properties properties) {
        if ( properties != null ) {
            this.chainedProperties.addProperties( properties );
        }
    }

    public void setProperty(String name,
                            String value) {
        name = name.trim();
        if ( StringUtils.isEmpty( name ) ) {
            return;
        }

        if ( name.equals( KeepReferenceOption.PROPERTY_NAME ) ) {
            setKeepReference( StringUtils.isEmpty( value ) ? true : Boolean.parseBoolean( value ) );
        } else if ( name.equals( ClockTypeOption.PROPERTY_NAME ) ) {
            setClockType( ClockType.resolveClockType( StringUtils.isEmpty( value ) ? "realtime" : value ) );
        } else if ( name.equals( QueryListenerOption.PROPERTY_NAME ) ) {
            setQueryListenerClass( StringUtils.isEmpty( value ) ? QueryListenerOption.STANDARD.getAsString() : value );
        }
    }

    public String getProperty(String name) {
        name = name.trim();
        if ( StringUtils.isEmpty( name ) ) {
            return null;
        }

        if ( name.equals( KeepReferenceOption.PROPERTY_NAME ) ) {
            return Boolean.toString( this.keepReference );
        } else if ( name.equals( ClockTypeOption.PROPERTY_NAME ) ) {
            return this.clockType.toExternalForm();
        } else if ( name.equals( QueryListenerOption.PROPERTY_NAME ) ) {
            return this.queryListener.getAsString();
        }
        return null;
    }

    /**
     * Makes the configuration object immutable. Once it becomes immutable,
     * there is no way to make it mutable again.
     * This is done to keep consistency.
     */
    public void makeImmutable() {
        this.immutable = true;
    }

    /**
     * Returns true if this configuration object is immutable or false otherwise.
     * @return
     */
    public boolean isImmutable() {
        return this.immutable;
    }

    private void checkCanChange() {
        if ( this.immutable ) {
            throw new UnsupportedOperationException( "Can't set a property after configuration becomes immutable" );
        }
    }

    public void setKeepReference(boolean keepReference) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.keepReference = keepReference;
    }

    public boolean isKeepReference() {
        return this.keepReference;
    }

    public ClockType getClockType() {
        return clockType;
    }

    public void setClockType(ClockType clockType) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.clockType = clockType;
    }

    @SuppressWarnings("unchecked")
    private void setQueryListenerClass(String property) {
        checkCanChange();
        this.queryListener = QueryListenerOption.determineQueryListenerClassOption( property );
    }

    private void setQueryListenerClass(QueryListenerOption option) {
        checkCanChange();
        this.queryListener = option;
    }

    public Map<String, WorkItemHandler> getWorkItemHandlers() {
        if ( this.workItemHandlers == null ) {
            initWorkItemHandlers();
        }
        return this.workItemHandlers;

    }

    private void initWorkItemHandlers() {
        this.workItemHandlers = new HashMap<String, WorkItemHandler>();

        // split on each space
        String locations[] = this.chainedProperties.getProperty( "drools.workItemHandlers",
                                                                 "" ).split( "\\s" );

        // load each SemanticModule
        for ( String factoryLocation : locations ) {
            // trim leading/trailing spaces and quotes
            factoryLocation = factoryLocation.trim();
            if ( factoryLocation.startsWith( "\"" ) ) {
                factoryLocation = factoryLocation.substring( 1 );
            }
            if ( factoryLocation.endsWith( "\"" ) ) {
                factoryLocation = factoryLocation.substring( 0,
                                                             factoryLocation.length() - 1 );
            }
            if ( !factoryLocation.equals( "" ) ) {
                loadWorkItemHandlers( factoryLocation );
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadWorkItemHandlers(String location) {
        String content = ConfFileUtils.URLContentsToString( ConfFileUtils.getURL( location,
                                                                                  null,
                                                                                  RuleBaseConfiguration.class ) );
        Map<String, WorkItemHandler> workItemHandlers = (Map<String, WorkItemHandler>) MVEL.eval( content,
                                                                                                  new HashMap() );
        this.workItemHandlers.putAll( workItemHandlers );
    }

    public WorkItemManagerFactory getWorkItemManagerFactory() {
        if ( this.workItemManagerFactory == null ) {
            initWorkItemManagerFactory();
        }
        return this.workItemManagerFactory;
    }

    @SuppressWarnings("unchecked")
    private void initWorkItemManagerFactory() {
        String className = this.chainedProperties.getProperty( "drools.workItemManagerFactory",
                                                               "org.drools.process.instance.impl.DefaultWorkItemManagerFactory" );
        Class<WorkItemManagerFactory> clazz = null;
        try {
            clazz = (Class<WorkItemManagerFactory>) this.classLoader.loadClass( className );
        } catch ( ClassNotFoundException e ) {
        }

        if ( clazz != null ) {
            try {
                this.workItemManagerFactory = clazz.newInstance();
            } catch ( Exception e ) {
                throw new IllegalArgumentException( "Unable to instantiate work item manager factory '" + className + "'",
                                                    e );
            }
        } else {
            throw new IllegalArgumentException( "Work item manager factory '" + className + "' not found" );
        }
    }

    public String getProcessInstanceManagerFactory() {
        return this.chainedProperties.getProperty( "drools.processInstanceManagerFactory",
                                                   "org.jbpm.process.instance.impl.DefaultProcessInstanceManagerFactory" );
    }

    public String getSignalManagerFactory() {
        return this.chainedProperties.getProperty( "drools.processSignalManagerFactory",
                                                   "org.jbpm.process.instance.event.DefaultSignalManagerFactory" );
    }

    public CommandService getCommandService(KnowledgeBase kbase,
                                            Environment environment) {
        if ( this.commandService == null ) {
            initCommandService( kbase,
                                environment );
        }
        return this.commandService;
    }

    @SuppressWarnings("unchecked")
    private void initCommandService(KnowledgeBase kbase,
                                    Environment environment) {
        String className = this.chainedProperties.getProperty( "drools.commandService",
                                                               null );
        if ( className == null ) {
            return;
        }

        Class<CommandService> clazz = null;
        try {
            clazz = (Class<CommandService>) this.classLoader.loadClass( className );
        } catch ( ClassNotFoundException e ) {
        }

        if ( clazz != null ) {
            try {
                this.commandService = clazz.getConstructor( KnowledgeBase.class,
                                                            KnowledgeSessionConfiguration.class,
                                                            Environment.class ).newInstance( kbase,
                                                                                             this,
                                                                                             environment );
            } catch ( Exception e ) {
                throw new IllegalArgumentException( "Unable to instantiate command service '" + className + "'",
                                                    e );
            }
        } else {
            throw new IllegalArgumentException( "Command service '" + className + "' not found" );
        }
    }

    public TimerService newTimerService() {
        String className = this.chainedProperties.getProperty(
                                                               "drools.timerService",
                                                               "org.drools.time.impl.JDKTimerService" );
        if ( className == null ) {
            return null;
        }

        Class<TimerService> clazz = null;
        try {
            clazz = (Class<TimerService>) this.classLoader.loadClass( className );
        } catch ( ClassNotFoundException e ) {
        }

        if ( clazz != null ) {
            try {
                return clazz.newInstance();
            } catch ( Exception e ) {
                throw new IllegalArgumentException(
                                                    "Unable to instantiate timer service '" + className
                                                            + "'",
                                                    e );
            }
        } else {
            throw new IllegalArgumentException( "Timer service '" + className
                                                + "' not found" );
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends SingleValueKnowledgeSessionOption> T getOption(Class<T> option) {
        if ( ClockTypeOption.class.equals( option ) ) {
            return (T) ClockTypeOption.get( getClockType().toExternalForm() );
        } else if ( KeepReferenceOption.class.equals( option ) ) {
            return (T) (this.keepReference ? KeepReferenceOption.YES : KeepReferenceOption.NO);
        } else if ( QueryListenerOption.class.equals( option ) ) {
            return (T) this.queryListener;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends MultiValueKnowledgeSessionOption> T getOption(Class<T> option,
                                                                    String key) {
        if ( WorkItemHandlerOption.class.equals( option ) ) {
            return (T) WorkItemHandlerOption.get( key,
                                                  getWorkItemHandlers().get( key ) );
        }
        return null;
    }

    public <T extends KnowledgeSessionOption> void setOption(T option) {
        if ( option instanceof ClockTypeOption ) {
            setClockType( ClockType.resolveClockType( ((ClockTypeOption) option).getClockType() ) );
        } else if ( option instanceof KeepReferenceOption ) {
            setKeepReference( ((KeepReferenceOption) option).isKeepReference() );
        } else if ( option instanceof WorkItemHandlerOption ) {
            getWorkItemHandlers().put( ((WorkItemHandlerOption) option).getName(),
                                       ((WorkItemHandlerOption) option).getHandler() );
        } else if ( option instanceof QueryListenerOption ) {
            this.queryListener = (QueryListenerOption) option;
        }
    }

    public ClassLoader getClassLoader() {
        return this.classLoader.clone();
    }

    public void setClassLoader(CompositeClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public QueryListenerOption getQueryListenerOption() {
        return this.queryListener;
    }

}
