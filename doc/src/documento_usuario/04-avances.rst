Avances
#######

.. contents::

O avance dun proxecto marca o grao no que se est� cumplindo co alcance estimado para a realizaci�n do mesmo, asimesmo, o avance dunha tarefa indica ese mesmo grao para o alcance estimado para dita tarefa.

Xeralmente os avances non te�en un modo autom�tico de ser medidos, e � unha persoa quen en base � experiencia ou � realizaci�n de unha lista de chequeo determina o grao de compleci�n de unha tarefa ou un proxecto.

Cabe destacar que hai unha diferencia importante entre a uso de horas asignadas a unha tarefa ou proxecto, co grao de avance nesa mesma tarefa ou proxecto. Mentres que o uso de horas pode estar en desv�o ou non, o proxecto pode estar nun grao de avance inferior � estimado para o d�a no que se est� controlando ou superior. Prod�cense, debido a estas d�as medidas, varias posibles situaci�ns:
   * Consum�ronse menos horas das estimadas para o elemento a medir e � mesmo tempo o proxecto est� indo m�is lento do estimado, porque o avance � inferior � estimado para o d�a de control.
   * Consum�ronse menos horas das estimadas para o elemento a medir e � mesmo tempo o proxecto est� indo m�is r�pido do estimado, porque o avance � inferior � estimado para o d�a de control.
   * Consum�ronse m�is horas das estimadas e � mesmo tempo o proxecto est� indo m�is lento do estimado, porque o avance � inferior � estimado para o d�a de control.
   * Consum�ronse m�is horas das estimadas e � mesmo tempo o proxecto est� indo m�is r�pido do estimado, porque o avance � inferior � estimado para o d�a de control.

O contraste de estas posibles situaci�ns � posible realizalo dende a propia planificaci�n, utilizando informaci�n do grao de avance e por outro lado do grao de uso de horas. Neste cap�tulo tratarase a introduci�n da informaci�n para poder levar un control do avance.

A filosof�a implantada no proxecto para o control do avance est� baseada en que o usuario divida ata o punto no que desexa o control de avances dos seus proxectos. En consecuencia, se o usuario desexa controlar a nivel de pedido, s� debe introducir informaci�n nos elementos de nivel 1, cando se desexa poder dispo�er de un control m�is fino sobre as tarefas, debe introducir informaci�n de avances en niveis inferiores, sendo o sistema que propaga cara arriba na xerarqu�a todos os datos.

Xesti�n de tipos de avance
--------------------------
Cada empresa pode ter unhas necesidades diferentes de control do avance dos seus proxectos, e concretamente das tarefas que os compo�en, por esta raz�n foi necesario contemplar a existencia dunhas entidades no sistema chamadas "tipos de avance". Os tipos de avance son diferentes tipolox�as que cada usuario pode dar de alta no sistema para medir o avance dunha tarefa. Por exemplo, unha tarefa pode ser medida porcentualmente, pero � mesmo tempo ese avance porcentual se traduce en un avance en *Toneladas* sobre o acordado co cliente.

Un tipo de avance est� caracterizado por un nome, un valor m�ximo e unha precisi�n:
   * Nome: Ser� un nome representativo que o usuario debe recordar para cando seleccione a asignaci�n de avances sexa capaz de entender qu� tipo de avance est� medindo.
   * Valor m�ximo: � o valor m�ximo que se lle permitir� a unha tarefa ou proxecto establecer como medida total de avance. Por exemplo, traballando con *Toneladas*, se se considera que o m�ximo normal en toneladas � de 4000 e nunca vai a haber tarefas que requiran realizar m�is de 4000 toneladas de alg�n material, ese deber�a ser o valor m�ximo establecido.
   * Precisi�n: � o valor dos incrementos que se permitir�n para o tipo de avance creado. Por exemplo, se o avance en *Toneladas* se vai a medir en valores redondeados, poder�a ser 1 a precisi�n. Dende ese momento, s� se poder�an introducir medidas de avance con n�meros enteiros, por exemplo, 1, 2, 300, etc.

O sistema conta con dous tipos de avance creados por defecto:
   * Porcentual: Tipo de avance xeral que permite medir o avance dun proxecto ou tarefa en base � porcentaxe que se estima de compleci�n do mesmo, por exemplo, unha tarefa est� � 30% respecto � 100% estimado nun d�a concreto.
   * Unidades: Tipo de avance xeral que permite medir o avance en unidades sen necesidade de especificar as unidades concretas. A tarefa comprend�a a creaci�n de 3000 unidades e o avance son 500 unidades sobre as 3000 estimadas.

O usuario poder� crear novos tipos de avance do seguinte xeito:
   * O usuario accede � secci�n de "Administraci�n".
   * Preme na opci�n do men� de segundo nivel "______".
   * O sistema amosa un listado de tipos de avance existentes.
   * Con cada tipo de avance o usuario poder�:
      * Editar
      * Borrar
   * A maiores, o usuario poder� crear un tipo de avance novo.
   * Coa edici�n ou a creaci�n, o sistema amosar� un formulario coa seguinte informaci�n:
      * Nome do tipo de avance.
      * Valor m�ximo que acepta o tipo de avance.
      * Precision di tipo de avance.


Introduci�n de avances en base a tipo
-------------------------------------



Contraste de avances sobre un elemento do pedido
------------------------------------------------

