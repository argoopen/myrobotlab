package org.myrobotlab.document.transformer;

import java.util.List;

import org.myrobotlab.document.Document;
import org.myrobotlab.document.transformer.wiki.TextConverter;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;
import org.sweble.wikitext.engine.EngineException;
import org.sweble.wikitext.engine.PageId;
import org.sweble.wikitext.engine.PageTitle;
import org.sweble.wikitext.engine.WtEngineImpl;
import org.sweble.wikitext.engine.config.WikiConfig;
import org.sweble.wikitext.engine.nodes.EngProcessedPage;
import org.sweble.wikitext.engine.utils.DefaultConfigEnWp;
import org.sweble.wikitext.parser.parser.LinkTargetException;

public class ParseWikiText extends AbstractStage {

  public final static Logger log = LoggerFactory.getLogger(ParseWikiText.class.getCanonicalName());
  String input = "text";
  String output = "text";
  @Override
  public void startStage(StageConfiguration config) {
    // TODO Auto-generated method stub
    input = config.getStringParam("input", input);
    output = config.getStringParam("output", output);
  }

  @Override
  public List<Document> processDocument(Document doc) {
    // TODO: what's the wiki id to use for this? does it even really matter?
    String title = doc.getId();
    if (!doc.hasField(input)) {
      log.info("no input field for wiki {}", doc.getId());
      return null;
    }
    String wikiText = doc.getField(input).get(0).toString();
    try {
      WikiConfig config = DefaultConfigEnWp.generate();
      // Instantiate a compiler for wiki pages
      WtEngineImpl engine = new WtEngineImpl(config);
      // Retrieve a page
      PageTitle pageTitle = PageTitle.make(config, title);
      PageId pageId = new PageId(pageTitle, -1);
      // Compile the retrieved page
      EngProcessedPage cp = engine.postprocess(pageId, wikiText, null);
      // This compiled page i think has all the mojo i seek!
      TextConverter p = new TextConverter(config, 132, doc);
      String result = (String)p.go(cp.getPage());
      doc.setField(output, result);
      // emit the children docs from this method.
      return p.getChildrenDocs();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public void stopStage() {
    // TODO Auto-generated method stub

  }

  @Override
  public void flush() {
    // TODO Auto-generated method stub

  }

}
