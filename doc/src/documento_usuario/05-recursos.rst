Xesti�n de recursos
###################

.. _recursos:
.. contents::

Xesti�n de recursos
===================

A aplicaci�n xestiona dous tipos de recursos diferentes: recursos traballadores e recursos m�quinas.

Os recursos traballadores representan os traballadores das empresas. As caracter�sticas principais son:
   * Satisfar�n un ou varios criterios de tipo xen�rico ou tipo traballador.
   * Son asignables espec�ficamente a unha tarefa.
   * Son asignables como parte da asignaci�n xen�rica a unha tarefa que requira un criterio que satisfai o traballador.
   * Dispor�n de un calendario por defecto ou espec�fico se as� o decide o usuario.

Os recursos m�quina representan as m�quinas das empresas. As caracter�sticas principais son:
   * Satisfar�n un ou varios criterios de tipo xen�rico ou tipo m�quina.
   * Son asignables espec�ficamente a unha tarefa.
   * Son asignables como parte da asignaci�n xen�rica a unha tarefa que requira un criterio que satisfai a m�quina.
   * Dispor�n de un calendario por defecto ou espec�fico se as� o decide o usuario.
   * Contar� un unha pantalla de configuraci�n na que se poder� establecer un valor *alfa* que represente a relaci�n entre m�quina e traballador.
      * O *alfa* representa canto tempo dun traballador � necesario para que a m�quina funcione. Por exemplo, un alfa de 0.5 indica que de cada 8 horas de m�quina son necesarias 4 de un traballador.
      * � posible asignar un *alfa* de xeito espec�fico a un traballador, � dicir, el�xese o traballador que estar� ocupado esa porcentaxe do seu tempo coa m�quina.
      * Ou ben, � posible facer unha asignaci�n xen�rica en base a un criterio, de xeito que se asigna unha porcentaxe do uso a todos os criterios que satisf�n ese criterio e te�en tempo dispo�ible. O funcionamento da asignaci�n xen�rica ser� a mesma que a explicada para asignaci�ns xen�ricas a tarefas.

O usuario poder� crear, editar e invalidar (nunca borrar definitivamente) traballadores da empresa dende a pestana de "Recursos". Dende dita pestana existen as seguintes operaci�ns:
   * Listado de traballadores: Os traballadores amosaranse listados e paxinados, dende onde poder�n xestionar os seus datos.
   * Listado de m�quinas: As m�quinas amosaranse listados e paxinados, dende onde poder�n xestionar os seus datos.

Xesti�n de traballadores
------------------------

A xesti�n de traballadores realizarase dende a pestana de "Recursos" e a operaci�n de "______". Dende a lista de recursos � posible editar cada un dos traballadores premendo na icona est�ndar de edici�n.

Unha vez na edici�n dun recurso, o usuario poder�:

1) Editar os datos b�sicos de identificaci�n do traballador.
      * Nome
      * Apelidos
      * DNI

.. figure:: images/worker-personal-data.png
   :scale: 70


2) Configurar os criterios que un traballador satisfai. O usuario poder� asignar calquera valor de criterio de tipo traballador ou xen�rico que as� considere a un traballador. � importante, para que a aplicaci�n sexa utilizada en todo o seu valor, que os traballadores satisfagan criterios. Para asignar criterios o usuario debe:

   i. Buscar o criterio que desexa engadir e seleccionar o que encaixe coa s�a procura.

   ii. Premer no bot�n de engadir.

   iii. Seleccionar data de inicio do criterio dende o momento que deba aplicarse.

   iv. Seleccionar a data de fin de aplicaci�n do criterio � recurso.

.. figure:: images/worker-criterions.png
   :scale: 70

3) Configurar un calendario espec�fico para o traballador.

.. figure:: images/worker-calendar.png
   :scale: 70



Xesti�n de m�quinas
-------------------

