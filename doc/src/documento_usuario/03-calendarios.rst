Calendarios
###########

.. contents::

Os calendarios ser�n os elementos do portal que determinen as dispo�ibilidades para o traballo dos distintos recursos. Un calendario na aplicaci�n estar� formado por unha serie de d�as anuais, cada d�a dispor� de unha serie de horas dispo�ibles para o traballo.

Por exemplo, un festivo ter� 0 horas dispo�ibles e, se as horas de traballo dentro dun d�a laboral son 8, ser� este n�mero o que se asignar� de dispo�ibilidade para ese d�a.

Administraci�n de calendarios
=============================

A administraci�n de calendarios actuar� do seguinte xeito:
   * Cada d�a � independente entre s� e cada ano ten d�as diferentes, � dicir, se se marca o 8 de Decembro de 2009 como festivo eso non quere dicir que o ano 2010 xa te�a o d�a 8 de Decembro como festivo.
   * Os d�as laborais m�rcanse en base a d�as da semana, � dicir, se se determina que o normal � traballar 8 horas os luns, quedar�n todos os luns de todas as semanas dos diferentes anos marcados como 8 horas dispo�ibles.
   * � posible marcar excepci�ns, � dicir, elixir un d�a concreto no que as horas dispo�ibles sexan diferentes � regla xeral para dito d�a da semana.

A administraci�n de calendarios est� accesible dende as operaci�ns de "Administraci�n". Desde dito punto o usuario pode realizar o seguinte
   1 Crear un novo calendario dende cero.
   1 Crear un calendario derivado de outro calendario.
   1 Crear un calendario como copia de outro calendario.
   1 Editar un calendario existente.

Creaci�n dun novo calendario
----------------------------

Para a creaci�n dun novo calendario � necesario premer no bot�n "______". O sistema amosar� un formulario no que o usuario poder� realizar as seguintes operaci�ns:
   * Marcar as horas dispo�ibles para cada d�a da semana (luns, martes, m�rcores, xoves, venres, s�bados e domingos).
   * Seleccionar un d�a espec�fico do calendario.
   * Asignar horas como excepci�n a un d�a espec�fico do calendario.

Con estas operaci�ns un usuario da aplicaci�n ten a capacidade de personalizar os calendarios completamente �s s�as necesidades. Para almacenar os cambios no formulario � necesario premer no bot�n "______".


Creaci�n dun calendario derivado
--------------------------------

Un calendario derivado � un calendario que se crea como derivaci�n dun existente, � dicir, herda todas as caracter�sticas do orixinal e � mesmo tempo � posible modificalo para que conte�a as s�as particularidades.

Un exemplo de uso de calendarios derivados � a existencia dun calendario xeral para Espa�a e a creaci�n dun derivado para s� incluir os 2 festivos galegos sobre o xeral.

� importante destacar que ante calquera modificaci�n realizada sobre o calendario orixinal o calendario derivado ser� directamente afectado, sempre e cando, non se definira unha actuaci�n concreta sobre el mesmo. Por exemplo, no calendario de Espa�a incl�ese un d�a laboral no 17 de Maio con 8 horas de traballo e no calendario galego, que se creou como derivaci�n, o d�a 17 de Maio � considerado un d�a de 0 horas de traballo por ser festivo. Se sobre o calendario espa�ol se cambian os d�as da semana do 17 Maio para que as horas dispo�ibles sexan 4 diarias, no galego o que suceder� � que todos os d�as da semana do 17 de Maio ter�n 4 horas dispo�ibles excepto o mesmo d�a 17 que ter� 0 horas, tal e como expl�citamente se establecera antes.

Para crear un calendario derivado na aplicaci�n, � necesario facer o seguinte:
   * Acceder � men� de *Administraci�n*.
   * Premer na operaci�n de administraci�n de calendarios.
   * Elixir un dos calendarios sobre o que se desexa realizar un derivado e premer no bot�n "______".
   * Unha vez realizada esta operaci�n o sistema amosar� un formulario de edici�n coas mesmas caracter�sticas que os formularios para crear calendarios dende cero, coa diferencia de que as excepci�ns e as horas por d�a da semana se propo�en en base � calendario orixinal.
 

Creaci�n dun calendario por copia
---------------------------------

Un calendario copiado � un calendario que se crea como copia exacta de outro existente, � dicir, que recibe todas as caracter�sticas do orixinal e � mesmo tempo � posible modificalo para que conte�a as s�as particularidades. 

A diferencia entre copiar e derivar un calendario radica nos cambios no orixinal. No caso de copias, se o orixinal � modificado, non afectar� � copia, sen embargo, cando se deriva, s� afecta � fillo.

Un exemplo de uso de calendario por copia � o dispor de un calendario para Pontevedra e necesitar un calendario para A Coru�a onde a maior�a das caracter�sticas son as mesmas sen embargo, non se espera que os cambios nun afecten � outro.

Para crear un calendario copiado na aplicaci�n, � necesario facer o seguinte:
   * Acceder � men� de *Administraci�n*.
   * Premer na operaci�n de administraci�n de calendarios.
   * Elixir un dos calendarios sobre o que se desexa realizar un derivado e premer no bot�n "______".
   * Unha vez realizada esta operaci�n o sistema amosar� un formulario de edici�n coas mesmas caracter�sticas que os formularios para crear calendarios dende cero, coa diferencia de que as excepci�ns e as horas por d�a da semana se propo�en en base � calendario orixinal.
 

Asignaci�n de calendario a recursos
-----------------------------------

Un recurso poder� recibir como asignaci�n un calendario existente ou un calendario creado espec�ficamente para o recurso. En calquera dos casos, unha vez se asigna un calendario a un recurso, � posible realizar modificaci�ns espec�ficas para ese recurso.

Os casos posibles son:
   * *Creaci�n de un calendario para o recurso a partir de cero*. Neste caso, o calendario ser� �nicamente para o recurso e calqueira caracter�stica que se lle desexe asignar deber� ser modificada no propio calendario do recurso.
   * *Creaci�n de un calendario para o recurso como copia de calendario existente*. O calendario recoller� dende un principio as especificidades do calendario orixinal, sen embargo, unha vez asignado, deber� ser modificado dende o propio recurso.
   * *Creaci�n de un calendario para o recurso como derivado de calendario existente*. O calendario recoller� dende un principio as especificidades do calendario orixinal e, � mesmo tempo, se o calendario orixinal � modificado para todos, o propio recurso recibir� esas modificaci�ns de xeito indirecto, tal e como se comentou na secci�n de calendarios derivados.

Para asignar un calendario a un recurso � necesario dar os seguintes pasos:
   * Acceder � edici�n de recursos.
   * Seleccionar a pestana de "______".
   * A partir da pestana anterior aparecer� un formulario de edici�n de calendarios que permitir�:
      * Crear un calendario dende cero premendo no bot�n "______".
      * Crear un calendario derivado premendo no bot�n "______".
      * Crear un bot�n como copia premendo no bot�n "______".
   * O sistema amosar� un formulario de edici�n de calendarios cos datos cargados en base � opci�n elixida no paso anterior.
   * ���Para almacenar � necsario premer en SAve????
