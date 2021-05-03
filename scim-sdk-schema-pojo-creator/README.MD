# Create POJO from SCIM schema file

This module is a simple helper that I created more or less in rush since I was sick of creating POJO's from schema
definitions. This has become hard work if done for several services and therefore this module provides a very simple
JavaFX UI that you can use to paste your schema and to generate a java POJO from it. 
            
Caution:

The tests of this module are pretty much crafted by hand and this module might break in future versions.

## How to use

Simply start the JavaFX mainclass `SchemaPojoTranslatorController`. This should start the UI with two text areas that
are described by labels. Paste your schema into the upper area and press the Button `translate` on the right side 
between these two text areas. The output should immediately appear in the below output.

Please note that the pasted schema is validated before parsed and exceptions will only be visible on system out. 
