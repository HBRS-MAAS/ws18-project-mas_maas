[![Codacy Badge](https://api.codacy.com/project/badge/Grade/1dc6be5861c74cdf92f44356da3b9ff6)](https://app.codacy.com/app/anenriquez/ws18-project-mas_maas?utm_source=github.com&utm_medium=referral&utm_content=HBRS-MAAS/ws18-project-mas_maas&utm_campaign=Badge_Grade_Dashboard)
[![Build Status](https://travis-ci.org/HBRS-MAAS/ws18-project-mas_maas.svg?branch=master)](https://travis-ci.org/HBRS-MAAS/ws18-project-mas_maas)
[![Coverage Status](https://coveralls.io/repos/github/HBRS-MAAS/ws18-project-mas_maas/badge.svg?branch=master)](https://coveralls.io/github/HBRS-MAAS/ws18-project-mas_maas?branch=master)
# MAAS Project - <Team mas_MAAS>

This project contains the dough preparation and baking stage for the MAAS WS 18/19. It uses proofer and coolingRack interface agents. Proofer connects dough preparation and baking stage, while coolingRack links the baking and packaging stage.

## Team Members
*  Erick Kramer - [erickkramer](https://github.com/erickkramer)
*   Angela Enriquez - [anenriquez](https://github.com/anenriquez)
*   Ethan Massey - [emassey2](https://github.com/emassey2)

## Dependencies
* JADE v.4.5.0
* ...

## How to run
Just install gradle and run:

    gradle run

It will automatically get the dependencies and start JADE with the configured agents.
In case you want to clean you workspace run

    gradle clean

## Eclipse
To use this project with eclipse run

    gradle eclipse

This command will create the necessary eclipse files.
Afterwards you can import the project folder.

## Run preparation stages in one computer

### Run only the **Dough Preparation Stage**

    gradle run --args=-doughPrep

### Run both **Dough Preparation** and **Baking Stage**

    gradle run --args='-doughPrep -bakingMasMaas -baking'

## Run preparation stages in different computers
