/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.gnoxy.kd_caller;

import com.gnoxy.k_data.dao.DAOFactory;
import com.gnoxy.k_data.dao.PersonDAO;
import com.gnoxy.k_data.elements.Person;
import java.io.File;
import java.sql.SQLException;
import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author pmaher
 */
public class Main {

    private static final Logger LOGGER = LogManager.getLogger();

    private static String connectionStringBase;
    private static String connectionType;

    public static void main(String[] args) throws SQLException {
        LOGGER.traceEntry();

        String propertiesFileName = null;
        boolean askForHelp = false;
        Options options = new Options();

        Option help = new Option("h", false, "print this message");
        options.addOption(help);

        Option propertiesFile = Option.builder("propertiesFile")
                .hasArg()
                .desc("the properties file to use")
                .build();
        options.addOption(propertiesFile);

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine line = parser.parse(options, args);

            if (line.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("Main", options);
                askForHelp = true;
            }

            if (line.hasOption("propertiesFile")) {
                propertiesFileName = line.getOptionValue("propertiesFile");
                LOGGER.info("Props file: " + propertiesFileName);
            }
        } catch (ParseException e) {
            LOGGER.error("Parsing failed. " + e.getMessage());
        }

        Configurations configs = new Configurations();
        if (propertiesFileName != null) {
            File props = new File(propertiesFileName);
            if (props.exists()) {
                try {
                    Configuration config = configs.properties(props);
                    connectionType = config.getString("connection.type");
                    connectionStringBase = config.getString("connection.string");

                    LOGGER.info("Using properties file: " + props);
                    LOGGER.info("connectionType: " + connectionType);
                    LOGGER.info("connectionStringBase: " + connectionStringBase);
                } catch (ConfigurationException e) {
                    LOGGER.error("Error: " + e.toString());
                }
            } else {
                LOGGER.fatal("Error: The specified properties file does not exist: " + propertiesFileName);
            }
        } else if (!askForHelp) {
            LOGGER.fatal("Error: A required properties file was not specified.");
        }

        String db_uname = System.getenv("DB_UNAME");
        String db_pw = System.getenv("DB_PW");

        if (StringUtils.isEmpty(db_uname) || StringUtils.isEmpty(db_pw)) {
            LOGGER.fatal("Error: The environment variables DB_UNAME or DB_PW are not set.");
        } else {
            String connectionString = connectionStringBase.
                    replaceAll("DB_UNAME", db_uname).replaceAll("DB_PW", db_pw);
            DAOFactory DFact = DAOFactory.getDAOFactory(connectionType, connectionString);
            PersonDAO DPerson = DFact.getPersonDAO();
            Set<Person> people = DPerson.getPeople();
            people.stream()
                    .forEach(e -> {
                        System.out.println(e.toString() + "\n");
                    });

            DFact.close();
        }
        LOGGER.traceExit();
    }

}
