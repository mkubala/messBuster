package pl.mkubala.messBuster.ui

import swing._
import java.awt.Color
import javax.swing.table.AbstractTableModel
import java.io.File
import scala.swing.FileChooser.Result
import pl.mkubala.Qmdt
import javax.swing.border.{CompoundBorder, EmptyBorder}
import javax.swing.BorderFactory
import scala.swing.event.TableRowsSelected

object MessBusterGUI extends SimpleSwingApplication {

  //  private lazy val dstDirChooser = buildDirChooser(false)

  //  def buildDirChooser(multiSelect: Boolean) = new FileChooser {
  //    fileSelectionMode = FileChooser.SelectionMode.DirectoriesOnly
  //    multiSelectionEnabled = multiSelect
  //  }

  private lazy val srcTableModel = new MyTableModel

  private lazy val mainLayout = new BoxPanel(Orientation.Vertical) {
    contents ++= Seq(
      Header,
      mainForms)
    //    ,
    //      Footer)
  }

  def setEnabled(isEnabled: Boolean) {
    mainForms.enabled = isEnabled
  }

  private val mainForms = new BoxPanel(Orientation.Vertical) with Margins {
    background = Color.WHITE
    border = margin

    private val targetChooser = new TargetDirChooser

    val runButton = new Button("Run") {
      action = Action("Run") {
        setEnabled(false)
        try {
          Qmdt.fire(srcTableModel.getData, targetChooser.getFile)
        } finally {
          setEnabled(true)
        }
      }
      //      enabled = false
    }

    val quitButton = new Button(Action("Quit") {
      quit()
    })

    contents ++= Seq(
      new SourceDirsChooser(srcTableModel).get,
      targetChooser.component,
      new BorderPanel with Margins {
        val bgColor = new Color(190, 190, 190)
        background = bgColor
        border = margin
        add(new BoxPanel(Orientation.Horizontal) {
          background = bgColor
          contents += runButton
          contents += quitButton
        }, BorderPanel.Position.East)
      }
    )
  }

  lazy val top = new MainFrame {
    title = "MessBuster"
    contents = mainLayout
  }

}

trait Margins {
  protected val margin = new EmptyBorder(10, 10, 10, 10)
}

object Header extends BorderPanel with Margins {
  background = Color.DARK_GRAY
  border = margin
  add(new Label {
    text = "### MessBuster"
    foreground = Color.WHITE
  }, BorderPanel.Position.West)
}

object Footer extends BorderPanel with Margins {
  val topBorder = BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY)
  border = new CompoundBorder(topBorder, margin)
  add(new Label {
    text = "MessBuster by Marcin Kubala"
  }, BorderPanel.Position.East)
}

class TargetDirChooser {

  private object state {
    private var value: File = _

    def get = new File(value.getAbsolutePath)

    def set(file: File) {
      value = file
      input.text = file match {
        case f: File => f.toString
        case _ => ""
      }
    }
  }

  def getFile = state.get

  private val chooser = new FileChooser {
    fileSelectionMode = FileChooser.SelectionMode.DirectoriesOnly
    multiSelectionEnabled = false
  }

  private val input = new TextField {
    enabled = false
  }

  private val button = new Button(
    Action("Choose..") {
      chooser.showOpenDialog(null) match {
        case Result.Approve => state set chooser.selectedFile
      }
    }
  )

  val component = new BoxPanel(Orientation.Horizontal) {
    contents += input
    contents += button
  }
}

class SourceDirsChooser(val tableModel: MyTableModel) {
  private val chooser = new FileChooser {
    fileSelectionMode = FileChooser.SelectionMode.DirectoriesOnly
    multiSelectionEnabled = true
  }

  private val addButton = new Button(
    Action("+") {
      chooser.showOpenDialog(null) match {
        case Result.Approve => tableModel.add(chooser.selectedFiles)
        case _ =>
      }
    }
  )

  private val table = new Table(Array[Array[Any]](), Array("Path")) {
    model = tableModel
  }

  private lazy val delButton = new Button("-") {
    enabled = false

    listenTo(table.selection)
    reactions += {
      case e: TableRowsSelected if e.source == table => {
        enabled = !table.selection.rows.isEmpty
      }
    }
    action = Action("-") {
      table.selection.rows foreach {
        rowIndex =>
          tableModel.deleteAt(rowIndex)
      }
    }
  }

  private val tableControlsPanel = new BorderPanel {
    add(new Label("Directories to scan")
      , BorderPanel.Position.West)
    add(new BoxPanel(Orientation.Horizontal) {
      contents += addButton
      contents += delButton
    }, BorderPanel.Position.East)
  }

  val get = new BoxPanel(Orientation.Vertical) {
    contents += tableControlsPanel
    contents += new ScrollPane(table)
  }

}

class MyTableModel extends AbstractTableModel {

  private lazy val data = scala.collection.mutable.ArrayBuffer[File]()

  def getColumnCount = 1

  def getRowCount: Int = data.size

  def getValueAt(p1: Int, p2: Int): AnyRef = data(p1)

  def add(files: Seq[File]) {
    data ++= files
    fireTableDataChanged()
  }

  def asStrings = (data map (_.toString)).toArray

  def getData: Seq[File] = Seq[File]() ++ data.toSeq

  def deleteAt(index: Int) {
    data.remove(index)
    fireTableDataChanged()
  }
}
