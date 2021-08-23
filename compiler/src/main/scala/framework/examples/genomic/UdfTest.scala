package framework.examples.genomic

import java.util.Base64
import framework.common._
import framework.examples.Query
import framework.nrc.Parser

// this is the Gene Mutation Burden Example
object ExampleQueryMultiOmicsProstate extends DriverGene {
  val sampleFile = "/mnt/app_hdd/data/biospecimen/aliquot/nationwidechildrens.org_biospecimen_aliquot_prad.txt"
  val cnvFile = "/mnt/app_hdd/data/cnv"
  val exprFile = "/mnt/app_hdd/data/expression/"
  val aexprFile = "/mnt/app_hdd/data/fpkm_uq_case_aliquot.txt"
  val occurFile = "/mnt/app_hdd/data/somatic/"
  val occurName = "datasetPRAD"
  val occurDicts = ("odictPrad1", "odictPrad2", "odictPrad3")
  val pathFile = "/mnt/app_hdd/data/pathway/c2.cp.v7.1.symbols.gmt"
  val gtfFile = "/mnt/app_hdd/data/genes/Homo_sapiens.GRCh37.87.chr.gtf"
  val pradFile = "/mnt/app_hdd/data/biospecimen/clinical/nationwidechildrens.org_clinical_patient_prad.txt"
  val clinDir = "/mnt/app_hdd/data/biospecimen/clinical"

  // in DriverGenes.scala you can see traits for several datatypes, these
  // are inherited from DriverGene trait (around line 549)
  // checkout individuals traits to see what the load functions are doing
//  Put next two lines in the loadTables when running tcga
//  val tcgaLoader = new TCGALoader(spark)
//  val tcgaData = tcgaLoader.load("/mnt/app_hdd/data/biospecimen/clinical", dir = true)
//
  override def loadTables(shred: Boolean = false, skew: Boolean = false): String =
    s"""|${loadBiospec(shred, skew, fname = pradFile, name = "clinical", func = "Prad")}
        |${loadBiospec(shred, skew, fname = sampleFile, name = "samples")}
        |${loadCopyNumber(shred, skew, fname = cnvFile)}
        |${loadGeneExpr(shred, skew, fname = exprFile, aname = aexprFile)}
        |${loadOccurrence(shred, skew, fname = occurFile, iname = occurName, dictNames = occurDicts)}
        |${loadPathway(shred, skew, fname = pathFile)}
        |${loadGtfTable(shred, skew, fname = gtfFile)}
        |""".stripMargin
  // name to identify your query
  val name = "ExampleQueryMultiOmicsProstate"
  // moved these to DriverGenes.scala Pathway train (line 19)
  // val genetype = TupleType("name" -> StringType)
  // val pathtype = TupleType("p_name" -> StringType, "url" -> StringType, "gene_set" -> BagType(genetype))

  // a map of input types for the parser
    val tbls = Map("occurrences" -> occurmids.tp,
                    "copynumber" -> copynum.tp,
                    "expression" -> fexpression.tp,
                    "samples" -> samples.tp,
                    "pathways" -> pathway.tp,
                    //"clinical" -> BagType(tcgaType),// this is for the tcga only
                    "genemap" -> gtf.tp,
                    "clinical" -> BagType(pradType))



  // a query string that is passed to the parser
  // note that a list of assignments should be separated with ";"
  // this was the query before labels	  
//  val query =
//    s"""
//      GMB <=
//          for g in genemap union
//            {(gene:= g.g_gene_name, burdens :=
//              (for o in occurrences union
//                for t in o.transcript_consequences union
//                  if (g.g_gene_id = t.gene_id) then
//                     {(sid := o.donorId, burden := if (t.impact = "HIGH") then 0.80
//                                                else if (t.impact = "MODERATE") then 0.50
//                                                else if (t.impact = "LOW") then 0.30
//                                                else 0.01)}).sumBy({sid}, {burden}))}
//     """
  val query = {

//// Using Occurences only (tcga loader)
      s"""   GMB <=
           for g in genemap union
             {(gene:= g.g_gene_name, burdens :=
               (for o in occurrences union
                 for s in clinical union
                  if (o.donorId = s.sample) then
                    for t in o.transcript_consequences union
                      if (g.g_gene_id = t.gene_id) then
                          {(sid := o.donorId,
                          lbl := if (s.tumor_tissue_site = "Breast") then 1
                                 else if (s.tumor_tissue_site = "Lung") then 2
                                 else if (s.tumor_tissue_site = "Kidney") then 3
                                 else if (s.tumor_tissue_site = "Stomach") then 4
                                 else if (s.tumor_tissue_site = "Ovary") then 5
                                 else if (s.tumor_tissue_site = "Endometrial") then 6
                                 else if (s.tumor_tissue_site = "Head and Neck") then 7
                                 else if (s.tumor_tissue_site = "Central nervous system") then 8
                                 else if (s.tumor_tissue_site = "Colon") then 0,
                                 else -1,
                          burden := if (t.impact = "HIGH") then 0.80
                                                 else if (t.impact = "MODERATE") then 0.50
                                                 else if (t.impact = "LOW") then 0.30
                                                 else 0.01)}).sumBy({sid, lbl}, {burden}))}
       """

// Using Occurences only (clinical biospec)
//    s"""
//
//        GMB <=
//          for g in genemap union
//            {(gene:= g.g_gene_name, burdens :=
//              (for o in occurrences union
//                for s in clinical union
//                  if (o.donorId = s.bcr_patient_uuid) then
//                    for t in o.transcript_consequences union
//                      if (g.g_gene_id = t.gene_id) then
//                         {(sid := o.donorId,
//                           lbl := if (s.gleason_pattern_primary = 2) then 0
//                            else if (s.gleason_pattern_primary = 3) then 0
//                            else if (s.gleason_pattern_primary = 4) then 1
//                            else if (s.gleason_pattern_primary = 5) then 1
//                            else -1,
//                           burden := if (t.impact = "HIGH") then 0.80
//                                                    else if (t.impact = "MODERATE") then 0.50
//                                                    else if (t.impact = "LOW") then 0.30
//                                                    else 0.01
//                          )}
//              ).sumBy({sid, lbl}, {burden})
//            )}
//    """


  // Using only Gene Expression (fpkm)
//  s"""
//        GMB <=
//          for g in genemap union
//            {(gene:= g.g_gene_name, fpkm :=
//              (for e in expression union
//                for c in clinical union
//                  for s in samples union
//                   if (s.bcr_patient_uuid = c.bcr_patient_uuid) then
//                    if (e.ge_aliquot = s.bcr_aliquot_uuid) then
//                      if (g.g_gene_id = e.ge_gene_id) then
//                         {(sid := e.ge_aliquot,
//                           lbl := if (c.gleason_pattern_primary = 2) then 0
//                            else if (c.gleason_pattern_primary = 3) then 0
//                            else if (c.gleason_pattern_primary = 4) then 1
//                            else if (c.gleason_pattern_primary = 5) then 1
//                            else -1,
//                           fpkm := e.ge_fpkm
//                          )}
//              ).sumBy({sid, lbl}, {fpkm})
//            )}
//    """


// Integrating occurences and gene expression
//
//    s"""
//        mapExpression <=
//          for s in samples union
//            for e in expression union
//              if (s.bcr_aliquot_uuid = e.ge_aliquot) then
//                {(sid := s.bcr_patient_uuid, gene := e.ge_gene_id, fpkm := e.ge_fpkm)};
//
//         impactGMB <=
//          for g in genemap union
//            {(gene_name := g.g_gene_name, gene_id:= g.g_gene_id, burdens :=
//              (for o in occurrences union
//                for s in clinical union
//                  if (o.donorId = s.bcr_patient_uuid) then
//                    for t in o.transcript_consequences union
//                      if (g.g_gene_id = t.gene_id) then
//                         {(sid := o.donorId,
//                           lbl := if (s.gleason_pattern_primary = 2) then 0
//                            else if (s.gleason_pattern_primary = 3) then 0
//                            else if (s.gleason_pattern_primary = 4) then 1
//                            else if (s.gleason_pattern_primary = 5) then 1
//                            else -1,
//                           burden := if (t.impact = "HIGH") then 0.80
//                                                    else if (t.impact = "MODERATE") then 0.50
//                                                    else if (t.impact = "LOW") then 0.30
//                                                    else 0.01
//                          )}
//              ).sumBy({sid, lbl}, {burden})
//            )};
//
//        GMB <=
//          for g in impactGMB union
//            {(gene_name := g.gene_name, gene_id := g.gene_id, burdens :=
//              (for b in g.burdens union
//                for e in mapExpression union
//                  if (b.sid = e.sid && g.gene_id = e.gene) then
//                    {(sid := b.sid, lbl := b.lbl, burden := b.burden*e.fpkm)}).sumBy({sid,lbl}, {burden})
//                     )}
//    """
//
////    s"""
//
//         GMB <=
//          for g in genemap union
//            {(gene_name := g.g_gene_name, burdens :=
//              (for o in occurrences union
//                for s in clinical union
//                  for e in expression union
//                    for s in samples union
//                     if (o.donorId = c.bcr_patient_uuid && s.bcr_patient_uuid = c.bcr_patient_uuid
//                         && e.ge_aliquot = s.bcr_aliquot_uuid) then
//                        for t in o.transcript_consequences union
//                          if (g.g_gene_id = t.gene_id) then
//                            {(sid := o.donorId,
//                              lbl := if (s.gleason_pattern_primary = 2) then 0
//                                else if (s.gleason_pattern_primary = 3) then 0
//                                else if (s.gleason_pattern_primary = 4) then 1
//                                else if (s.gleason_pattern_primary = 5) then 1
//                                else -1,
//                           burden := (e.ge_fpkm + 0.001) * if (t.impact = "HIGH") then 0.80
//                                                           else if (t.impact = "MODERATE") then 0.50
//                                                           else if (t.impact = "LOW") then 0.30
//                                                           else 0.01
//                          )}
//              ).sumBy({sid, lbl}, {burden})
//            )};
//
//
//    """


  }
  // finally define the parser, note that it takes the input types
    // map as input and pass the query string to the parser to
    // generate the program.
    val parser = Parser(tbls)
    val program = parser.parse(query).get.asInstanceOf[Program]

}

object ExampleQueryMultiTcga extends DriverGene {
  val sampleFile = "/mnt/app_hdd/data/biospecimen/aliquot/nationwidechildrens.org_biospecimen_aliquot_prad.txt"
  val cnvFile = "/mnt/app_hdd/data/cnv"
  val exprFile = "/mnt/app_hdd/data/expression/"
  val aexprFile = "/mnt/app_hdd/data/fpkm_uq_case_aliquot.txt"
  val occurFile = "/mnt/app_hdd/data/somatic/"
  // For the tcga loader
  val occurName = "datasetFull"
  val occurDicts = ("odictMutect1", "odictMutect2", "odictMutect3")
  // val occurDicts = ("cancerDict1", "cancerDict2", "cancerDict3")
  val pathFile = "/mnt/app_hdd/data/pathway/c2.cp.v7.1.symbols.gmt"
  val gtfFile = "/mnt/app_hdd/data/genes/Homo_sapiens.GRCh37.87.chr.gtf"
  val pradFile = "/mnt/app_hdd/data/biospecimen/clinical/nationwidechildrens.org_clinical_patient_prad.txt"
  val clinDir = "/mnt/app_hdd/data/biospecimen/clinical"

  // in DriverGenes.scala you can see traits for several datatypes, these
  // are inherited from DriverGene trait (around line 549)
  // checkout individuals traits to see what the load functions are doing
  //  Put next two lines in the loadTables when running tcga

  //
  override def loadTables(shred: Boolean = false, skew: Boolean = false): String =
    s"""|val tcgaLoader = new TCGALoader(spark)
        |val tcgaData = tcgaLoader.load("/mnt/app_hdd/data/biospecimen/clinical", dir = true)
        |${loadBiospec(shred, skew, fname = sampleFile, name = "samples")}
        |${loadCopyNumber(shred, skew, fname = cnvFile)}
        |${loadGeneExpr(shred, skew, fname = exprFile, aname = aexprFile)}
        |${loadOccurrence(shred, skew, fname = occurFile, iname = occurName, dictNames = occurDicts)}
        |${loadPathway(shred, skew, fname = pathFile)}
        |${loadGtfTable(shred, skew, fname = gtfFile)}
        |""".stripMargin
  // name to identify your query
  val name = "ExampleQueryMultiTcga"
  // moved these to DriverGenes.scala Pathway train (line 19)
  // val genetype = TupleType("name" -> StringType)
  // val pathtype = TupleType("p_name" -> StringType, "url" -> StringType, "gene_set" -> BagType(genetype))

  // a map of input types for the parser
  val tbls = Map("occurrences" -> occurmids.tp,
    "copynumber" -> copynum.tp,
    "expression" -> fexpression.tp,
    "samples" -> samples.tp,
    "pathways" -> pathway.tp,
    "clinical" -> BagType(tcgaType),// this is for the tcga only
    "genemap" -> gtf.tp)
//    "clinical" -> BagType(pradType))



  // a query string that is passed to the parser
  // note that a list of assignments should be separated with ";"
  // this was the query before labels
  /**val query =
    s"""
      GMB <=
          for g in genemap union
            {(gene:= g.g_gene_name, burdens :=
              (for o in occurrences union
                for t in o.transcript_consequences union
                  if (g.g_gene_id = t.gene_id) then
                     {(sid := o.donorId, burden := if (t.impact = "HIGH") then 0.80
                                                else if (t.impact = "MODERATE") then 0.50
                                                else if (t.impact = "LOW") then 0.30
                                                else 0.01)}).sumBy({sid}, {burden}))}
     """**/
  val query = {
    // notes from discussion
    // s"""
    //     // defined some udfs
    //     def pivot(df): ... // todo
    //     def sepDF(df): (X,y) // todo
    //     def chi_square(X,y): ... // todo


    //     GMB <=
    //       for g in genemap union
    //         {(gene:= g.g_gene_name, burdens :=
    //           (for o in occurrences union
    //             for t in o.transcript_consequences union
    //               if (g.g_gene_id = t.gene_id) then
    //                  {(sid := o.donorId, burden := if (t.impact = "HIGH") then 0.80
    //                                             else if (t.impact = "MODERATE") then 0.50
    //                                             else if (t.impact = "LOW") then 0.30
    //                                             else 0.01)}).sumBy({sid}, {burden}))}
    //     matrix <= pivot(GMB)
    //     (X,y) <= sepDF(matrix)
    //     selected_genes <= chi_square(X,y) // user could specify a gene set
    //     matrix <= matrix[selected_genes] // subset
    // """


    // Using Occurences only (tcga loader)

          s"""   GMB <=
               for g in genemap union
                 {(gene:= g.g_gene_name, burdens :=
                   (for o in occurrences union
                     for s in clinical union
                      if (o.donorId = s.sample) then
                        for t in o.transcript_consequences union
                          if (g.g_gene_id = t.gene_id) then
                              {(sid := o.donorId,
                                lbl := lbl := if (s.tumor_tissue_site = "Breast") then 1
                                         `  else if (s.tumor_tissue_site = "Lung") then 2
                                            else if (s.tumor_tissue_site = "Kidney") then 3
                                            else if (s.tumor_tissue_site = "Stomach") then 4
                                            else if (s.tumor_tissue_site = "Ovary") then 5
                                            else if (s.tumor_tissue_site = "Endometrial") then 6
                                            else if (s.tumor_tissue_site = "Head and Neck") then 7
                                            else if (s.tumor_tissue_site = "Central nervous system") then 8
                                            else if (s.tumor_tissue_site = "Colon") then 0
                                            else 9,
                                burden := if (t.impact = "HIGH") then 0.80
                                                     else if (t.impact = "MODERATE") then 0.50
                                                     else if (t.impact = "LOW") then 0.30
                                                     else 0.01)}).sumBy({sid, lbl}, {burden}))}
           """

    // Using only Gene Expression (fpkm)
    //  s"""
    //        GMB <=
    //          for g in genemap union
    //            {(gene:= g.g_gene_name, fpkm :=
    //              (for e in expression union
    //                for c in clinical union
    //                  for s in samples union
    //                   if (s.bcr_patient_uuid = c.bcr_patient_uuid) then
    //                    if (e.ge_aliquot = s.bcr_aliquot_uuid) then
    //                      if (g.g_gene_id = e.ge_gene_id) then
    //                         {(sid := e.ge_aliquot,
    //                           lbl := if (c.gleason_pattern_primary = 2) then 0
    //                            else if (c.gleason_pattern_primary = 3) then 0
    //                            else if (c.gleason_pattern_primary = 4) then 1
    //                            else if (c.gleason_pattern_primary = 5) then 1
    //                            else -1,
    //                           fpkm := e.ge_fpkm
    //                          )}
    //              ).sumBy({sid, lbl}, {fpkm})
    //            )}
    //    """


    // Integrating occurences and gene expression (tcga)

//        s"""
//            mapExpression <=
//              for s in samples union
//                for e in expression union
//                  if (s.bcr_aliquot_uuid = e.ge_aliquot) then
//                    {(sid := s.bcr_patient_uuid, gene := e.ge_gene_id, fpkm := e.ge_fpkm)};
//
//             impactGMB <=
//              for g in genemap union
//                {(gene_name := g.g_gene_name, gene_id:= g.g_gene_id, burdens :=
//                  (for o in occurrences union
//                    for s in clinical union
//                      if (o.donorId = s.sample) then
//                        for t in o.transcript_consequences union
//                          if (g.g_gene_id = t.gene_id) then
//                             {(sid := o.donorId,
//                               lbl := if (s.tumor_tissue_site = "Breast") then 1
//                                         else if (s.tumor_tissue_site = "Lung") then 2
//                                         else if (s.tumor_tissue_site = "Kidney") then 3
//                                         else if (s.tumor_tissue_site = "Stomach") then 4
//                                         else if (s.tumor_tissue_site = "Ovary") then 5
//                                         else if (s.tumor_tissue_site = "Endometrial") then 6
//                                         else if (s.tumor_tissue_site = "Head and Neck") then 7
//                                         else if (s.tumor_tissue_site = "Central nervous system") then 8
//                                         else if (s.tumor_tissue_site = "Colon") then 0
//                                     else 9,
//                               burden := if (t.impact = "HIGH") then 0.80
//                                                        else if (t.impact = "MODERATE") then 0.50
//                                                        else if (t.impact = "LOW") then 0.30
//                                                        else 0.01
//                              )}
//                  ).sumBy({sid, lbl}, {burden})
//                )};
//
//            GMB <=
//              for g in impactGMB union
//                {(gene_name := g.gene_name, gene_id := g.gene_id, burdens :=
//                  (for b in g.burdens union
//                    for e in mapExpression union
//                      if (b.sid = e.sid && g.gene_id = e.gene) then
//                        {(sid := b.sid, lbl := b.lbl, burden := b.burden*e.fpkm)}).sumBy({sid,lbl}, {burden})
//                         )}
//        """


  }
  // finally define the parser, note that it takes the input types
  // map as input and pass the query string to the parser to
  // generate the program.
  val parser = Parser(tbls)
  val program = parser.parse(query).get.asInstanceOf[Program]

}

object ExampleQueryBinaryTcga extends DriverGene {
  val sampleFile = "/mnt/app_hdd/data/biospecimen/aliquot/nationwidechildrens.org_biospecimen_aliquot_prad.txt"
  val cnvFile = "/mnt/app_hdd/data/cnv"
  val exprFile = "/mnt/app_hdd/data/expression/"
  val aexprFile = "/mnt/app_hdd/data/fpkm_uq_case_aliquot.txt"
  val occurFile = "/mnt/app_hdd/data/somatic/"
  // For the tcga loader
  val occurName = "datasetFull"
  val occurDicts = ("odictMutect1", "odictMutect2", "odictMutect3")
  // val occurDicts = ("cancerDict1", "cancerDict2", "cancerDict3")
  val pathFile = "/mnt/app_hdd/data/pathway/c2.cp.v7.1.symbols.gmt"
  val gtfFile = "/mnt/app_hdd/data/genes/Homo_sapiens.GRCh37.87.chr.gtf"
  val pradFile = "/mnt/app_hdd/data/biospecimen/clinical/nationwidechildrens.org_clinical_patient_prad.txt"
  val clinDir = "/mnt/app_hdd/data/biospecimen/clinical"

  // in DriverGenes.scala you can see traits for several datatypes, these
  // are inherited from DriverGene trait (around line 549)
  // checkout individuals traits to see what the load functions are doing
  //  Put next two lines in the loadTables when running tcga

  //
  override def loadTables(shred: Boolean = false, skew: Boolean = false): String =
    s"""|val tcgaLoader = new TCGALoader(spark)
        |val tcgaData = tcgaLoader.load("/mnt/app_hdd/data/biospecimen/clinical", dir = true)
        |${loadBiospec(shred, skew, fname = sampleFile, name = "samples")}
        |${loadCopyNumber(shred, skew, fname = cnvFile)}
        |${loadGeneExpr(shred, skew, fname = exprFile, aname = aexprFile)}
        |${loadOccurrence(shred, skew, fname = occurFile, iname = occurName, dictNames = occurDicts)}
        |${loadPathway(shred, skew, fname = pathFile)}
        |${loadGtfTable(shred, skew, fname = gtfFile)}
        |""".stripMargin
  // name to identify your query
  val name = "ExampleQueryMultiTcga"
  // moved these to DriverGenes.scala Pathway train (line 19)
  // val genetype = TupleType("name" -> StringType)
  // val pathtype = TupleType("p_name" -> StringType, "url" -> StringType, "gene_set" -> BagType(genetype))

  // a map of input types for the parser
  val tbls = Map("occurrences" -> occurmids.tp,
    "copynumber" -> copynum.tp,
    "expression" -> fexpression.tp,
    "samples" -> samples.tp,
    "pathways" -> pathway.tp,
    "clinical" -> BagType(tcgaType),// this is for the tcga only
    "genemap" -> gtf.tp)
  //    "clinical" -> BagType(pradType))


  val query = {

    // Using Occurences only (tcga loader)

    s"""   GMB <=
               for g in genemap union
                 {(gene:= g.g_gene_name, burdens :=
                   (for o in occurrences union
                     for s in clinical union
                      if (o.donorId = s.sample) then
                        for t in o.transcript_consequences union
                          if (g.g_gene_id = t.gene_id) then
                              {(sid := o.donorId,
                              lbl := s.tumor_tissue_site,
                              burden := if (t.impact = "HIGH") then 0.80
                                                     else if (t.impact = "MODERATE") then 0.50
                                                     else if (t.impact = "LOW") then 0.30
                                                     else 0.01)}).sumBy({sid, lbl}, {burden}))}
           """

  }
  // finally define the parser, note that it takes the input types
  // map as input and pass the query string to the parser to
  // generate the program.
  val parser = Parser(tbls)
  val program = parser.parse(query).get.asInstanceOf[Program]

}

object MultiTcga extends DriverGene {
  val sampleFile = "/mnt/app_hdd/data/biospecimen/aliquot/nationwidechildrens.org_biospecimen_aliquot_prad.txt"
  val cnvFile = "/mnt/app_hdd/data/cnv"
  val exprFile = "/mnt/app_hdd/data/expression/"
  val aexprFile = "/mnt/app_hdd/data/fpkm_uq_case_aliquot.txt"
  val occurFile = "/mnt/app_hdd/data/somatic/"
  // For the tcga loader
  val occurName = "datasetFull"
  val occurDicts = ("odictMutect1", "odictMutect2", "odictMutect3")
  // val occurDicts = ("cancerDict1", "cancerDict2", "cancerDict3")
  val pathFile = "/mnt/app_hdd/data/pathway/c2.cp.v7.1.symbols.gmt"
  val gtfFile = "/mnt/app_hdd/data/genes/Homo_sapiens.GRCh37.87.chr.gtf"
  val pradFile = "/mnt/app_hdd/data/biospecimen/clinical/nationwidechildrens.org_clinical_patient_prad.txt"
  val clinDir = "/mnt/app_hdd/data/biospecimen/clinical"

  // in DriverGenes.scala you can see traits for several datatypes, these
  // are inherited from DriverGene trait (around line 549)
  // checkout individuals traits to see what the load functions are doing
  //  Put next two lines in the loadTables when running tcga

  //
  override def loadTables(shred: Boolean = false, skew: Boolean = false): String =
    s"""|val tcgaLoader = new TCGALoader(spark)
        |val tcgaData = tcgaLoader.load("/mnt/app_hdd/data/biospecimen/clinical", dir = true)
        |${loadBiospec(shred, skew, fname = sampleFile, name = "samples")}
        |${loadCopyNumber(shred, skew, fname = cnvFile)}
        |${loadGeneExpr(shred, skew, fname = exprFile, aname = aexprFile)}
        |${loadOccurrence(shred, skew, fname = occurFile, iname = occurName, dictNames = occurDicts)}
        |${loadPathway(shred, skew, fname = pathFile)}
        |${loadGtfTable(shred, skew, fname = gtfFile)}
        |""".stripMargin
  // name to identify your query
  val name = "MultiTcga"
  // moved these to DriverGenes.scala Pathway train (line 19)
  // val genetype = TupleType("name" -> StringType)
  // val pathtype = TupleType("p_name" -> StringType, "url" -> StringType, "gene_set" -> BagType(genetype))

  // a map of input types for the parser
  val tbls = Map("occurrences" -> occurmids.tp,
    "copynumber" -> copynum.tp,
    "expression" -> fexpression.tp,
    "samples" -> samples.tp,
    "pathways" -> pathway.tp,
    "clinical" -> BagType(tcgaType),// this is for the tcga only
    "genemap" -> gtf.tp)
  //    "clinical" -> BagType(pradType))


  val query = {

    // Using Occurences only (tcga loader)

    s"""   GMB <=
               for g in genemap union
                 {(gene:= g.g_gene_name, burdens :=
                   (for o in occurrences union
                     for s in clinical union
                      if (o.donorId = s.sample) then
                        for t in o.transcript_consequences union
                          if (g.g_gene_id = t.gene_id) then
                              {(sid := o.donorId,
                                lbl := if (s.tumor_tissue_site = "Breast") then 1
                                             else if (s.tumor_tissue_site = "Lung") then 2
                                             else if (s.tumor_tissue_site = "Kidney") then 3
                                             else if (s.tumor_tissue_site = "Stomach") then 4
                                             else if (s.tumor_tissue_site = "Ovary") then 5
                                             else if (s.tumor_tissue_site = "Endometrial") then 6
                                             else if (s.tumor_tissue_site = "Head and Neck") then 7
                                             else if (s.tumor_tissue_site = "Central nervous system") then 8
                                             else if (s.tumor_tissue_site = "Colon") then 0
                                             else 9,
                              burden := if (t.impact = "HIGH") then 0.80
                                                     else if (t.impact = "MODERATE") then 0.50
                                                     else if (t.impact = "LOW") then 0.30
                                                     else 0.01)}).sumBy({sid, lbl}, {burden}))}
           """

  }
  // finally define the parser, note that it takes the input types
  // map as input and pass the query string to the parser  to
  // generate the program.
  val parser = Parser(tbls)
  val program = parser.parse(query).get.asInstanceOf[Program]

}

// this is Pathway Mutation Burden Example
object ExampleQuery2 extends DriverGene {

  val sampleFile = "/mnt/app_hdd/data/biospecimen/aliquot/nationwidechildrens.org_biospecimen_aliquot_prad.txt"
  val cnvFile = "/mnt/app_hdd/data/cnv"
  val occurFile = "/mnt/app_hdd/data/somatic/"
  val occurName = "datasetPRAD"
  val occurDicts = ("odictPrad1", "odictPrad2", "odictPrad3")
  val pathFile = "/mnt/app_hdd/data/pathway/c2.cp.v7.1.symbols.gmt"
  val gtfFile = "/mnt/app_hdd/data/genes/Homo_sapiens.GRCh37.87.chr.gtf"
  val pradFile = "/mnt/app_hdd/data/biospecimen/clinical/nationwidechildrens.org_clinical_patient_prad.txt"


  // in DriverGenes.scala you can see traits for several datatypes, these
  // are inherited from DriverGene trait (around line 549)
  // checkout individuals traits to see what the load functions are doing
  override def loadTables(shred: Boolean = false, skew: Boolean = false): String =
    s"""|${loadBiospec(shred, skew, fname = sampleFile, name = "samples")}
        |${loadBiospec(shred, skew, fname = pradFile, name = "clinical", func = "Prad")}
        |${loadCopyNumber(shred, skew, fname = cnvFile)}
        |${loadOccurrence(shred, skew, fname = occurFile, iname = occurName, dictNames = occurDicts)}
        |${loadPathway(shred, skew, fname = pathFile)}
        |${loadGtfTable(shred, skew, fname = gtfFile)}
        |""".stripMargin

  // name to identify your query
  val name = "ExampleQuery2"

  // a map of input types for the parser
  val tbls = Map("occurrences" -> occurmids.tp,
    "copynumber" -> copynum.tp,
    "samples" -> samples.tp,
    "pathways" -> pathway.tp,
    "genemap" -> gtf.tp)


  // a query string that is passed to the parser
  // note that a list of assignments should be separated with ";"
  val query =
    s"""
        impactScores <=
          (for o in occurrences union
            for t in o.transcript_consequences union
              {(gid := t.gene_id, sid := o.donorId, burden := if (t.impact = "HIGH") then 0.80
                                                else if (t.impact = "MODERATE") then 0.50
                                                else if (t.impact = "LOW") then 0.30
                                                else 0.01)}).sumBy({gid, sid}, {burden});
        PMB <=
          for p in pathways union
            {(pathway := p.p_name, burdens :=
              (for g in p.gene_set union
                for g2 in genemap union
                  if (g.name = g2.g_gene_name) then
                    for o in impactScores union
                      if (g2.g_gene_id = o.gid) then
                        {(sid := o.sid, burden := o.burden)}).sumBy({sid}, {burden}))}

    """

    // finally define the parser, note that it takes the input types
    // map as input and pass the query string to the parser to
    // generate the program.
    val parser = Parser(tbls)
    val program = parser.parse(query).get.asInstanceOf[Program]

}

object SimpleUDFExample extends DriverGene {

  val sampleFile = "/mnt/app_hdd/data/biospecimen/aliquot/nationwidechildrens.org_biospecimen_aliquot_prad.txt"

  // in DriverGenes.scala you can see traits for several datatypes, these
  // are inherited from DriverGene trait (around line 549)
  // checkout individuals traits to see what the load functions are doing
  override def loadTables(shred: Boolean = false, skew: Boolean = false): String =
    s"""|${loadBiospec(shred, skew, fname = sampleFile, name = "samples")}
        |""".stripMargin

  // name to identify your query
  val name = "SimpleUDF"

  // a map of input types for the parser
  val tbls = Map("samples" -> samples.tp)


  // the parser part will be a bit involved, so i'm starting with
  // directly defining the queries as their case classes
  // val query =
  //   s"""
  //     def myudf input String output String
  //     Example <=
  //       for s in samples union
  //         {( sample := myudf(s.bcr_patient_uuid), aliquot := s.bcr_aliquot_uuid )}
  //   """

  // let's say we define a udf (myudf) that takes an input string
  // and outputs an input string - so it will take a patient id and
  // translate it to something like "JustTesting"
  val query = ForeachUnion(br, samples,
    Singleton(Tuple(
      "sample" -> PrimitiveUdf("myudf", br("bcr_patient_uuid"), StringType,Nil,"hint test"),
      //"sample" -> NumericUdf("udf_numeric", br("bcr_patient_uuid"), LongType),
      "aliquot" -> br("bcr_aliquot_uuid"))))

  val program = Program(Assignment(name, query))

}
//
//object BagUDFExample extends DriverGene {
//
//  val sampleFile = "/mnt/app_hdd/data/biospecimen/aliquot/nationwidechildrens.org_biospecimen_aliquot_prad.txt"
//
//  // in DriverGenes.scala you can see traits for several datatypes, these
//  // are inherited from DriverGene trait (around line 549)
//  // checkout individuals traits to see what the load functions are doing
//  override def loadTables(shred: Boolean = false, skew: Boolean = false): String =
//    s"""|${loadBiospec(shred, skew, fname = sampleFile, name = "samples")}
//        |""".stripMargin
//
//  // name to identify your query
//  val name = "BagUDF"
//
//  // a map of input types for the parser
//  val tbls = Map("samples" -> samples.tp)
//
//  val query = ForeachUnion(br, samples,
//    Singleton(Tuple(
//      "sample" -> BagUdf("myudf", br("bcr_patient_uuid"), StringType),
//      "aliquot" -> br("bcr_aliquot_uuid"))))
//
//  val program = Program(Assignment(name, query))
//
//}

object BagUDFExample extends DriverGene {

  val sampleFile = "/mnt/app_hdd/data/biospecimen/aliquot/nationwidechildrens.org_biospecimen_aliquot_prad.txt"
  val cnvFile = "/mnt/app_hdd/data/cnv"
  val exprFile = "/mnt/app_hdd/data/expression/"
  val aexprFile = "/mnt/app_hdd/data/fpkm_uq_case_aliquot.txt"
  val occurFile = "/mnt/app_hdd/data/somatic/"
  val occurName = "datasetPRAD"
  val occurDicts = ("odictPrad1", "odictPrad2", "odictPrad3")
  val pathFile = "/mnt/app_hdd/data/pathway/c2.cp.v7.1.symbols.gmt"
  val gtfFile = "/mnt/app_hdd/data/genes/Homo_sapiens.GRCh37.87.chr.gtf"
  val pradFile = "/mnt/app_hdd/data/biospecimen/clinical/nationwidechildrens.org_clinical_patient_prad.txt"

  override def loadTables(shred: Boolean = false, skew: Boolean = false): String =
    s"""|${loadBiospec(shred, skew, fname = pradFile, name = "clinical", func = "Prad")}
        |${loadBiospec(shred, skew, fname = sampleFile, name = "samples")}
        |${loadCopyNumber(shred, skew, fname = cnvFile)}
        |${loadGeneExpr(shred, skew, fname = exprFile, aname = aexprFile)}
        |${loadOccurrence(shred, skew, fname = occurFile, iname = occurName, dictNames = occurDicts)}
        |${loadPathway(shred, skew, fname = pathFile)}
        |${loadGtfTable(shred, skew, fname = gtfFile)}
        |""".stripMargin


  val name = "BagUDFQuery"

  // a map of input types for the parser
    val tbls = Map("occurrences" -> occurmids.tp,
                    "copynumber" -> copynum.tp,
                    "expression" -> fexpression.tp,
                    "samples" -> samples.tp,
                    "pathways" -> pathway.tp,
                    "genemap" -> gtf.tp, 
                    "clinical" -> BagType(pradType))


  val query =

    s"""
        mapExpression <=
          for s in samples union
            for e in expression union
              if (s.bcr_aliquot_uuid = e.ge_aliquot) then
                {(sid := s.bcr_patient_uuid, gene := e.ge_gene_id, fpkm := e.ge_fpkm)};

         impactGMB <=
          for g in genemap union
            {(gene_name := g.g_gene_name, gene_id:= g.g_gene_id, burdens :=
              (for o in occurrences union
                for s in clinical union
                  if (o.donorId = s.bcr_patient_uuid) then
                    for t in o.transcript_consequences union
                      if (g.g_gene_id = t.gene_id) then
                         {(sid := o.donorId,
                           lbl := if (s.gleason_pattern_primary = 2) then 0
                            else if (s.gleason_pattern_primary = 3) then 0
                            else if (s.gleason_pattern_primary = 4) then 1
                            else if (s.gleason_pattern_primary = 5) then 1
                            else -1,
                           burden := if (t.impact = "HIGH") then 0.80
                                                    else if (t.impact = "MODERATE") then 0.50
                                                    else if (t.impact = "LOW") then 0.30
                                                    else 0.01
                          )}
              ).sumBy({sid, lbl}, {burden})
            )};

        GMB <=
          for g in impactGMB union
            {(gene_name := g.gene_name, gene_id := g.gene_id, burdens :=
              (for b in g.burdens union
                for e in mapExpression union
                  if (b.sid = e.sid && g.gene_id = e.gene) then
                    {(sid := b.sid, lbl := b.lbl, burden := b.burden*e.fpkm)}).sumBy({sid,lbl}, {burden})
                     )};

        Final <= countudf_IntType(GMB)
    """

    // note for now, the type will need to be the last component of the function name separated by a _

    val parser = Parser(tbls)
    val program = parser.parse(query).get.asInstanceOf[Program]

}

object TupleUDFExample extends DriverGene {

  val sampleFile = "/mnt/app_hdd/data/biospecimen/aliquot/nationwidechildrens.org_biospecimen_aliquot_prad.txt"

  // in DriverGenes.scala you can see traits for several datatypes, these
  // are inherited from DriverGene trait (around line 549)
  // checkout individuals traits to see what the load functions are doing
  override def loadTables(shred: Boolean = false, skew: Boolean = false): String =
    s"""|${loadBiospec(shred, skew, fname = sampleFile, name = "samples")}
        |""".stripMargin

  // name to identify your query
  val name = "SimpleUDF"

  // a map of input types for the parser
  val tbls = Map("samples" -> samples.tp)

  val otp = TupleType("one" -> StringType, "two" -> StringType)

  val otp1 = TupleType(("one", StringType), ("two", StringType))

  val query = ForeachUnion(br, samples, 
    Singleton(
      Tuple(
      "sample" -> Singleton(
        TupleUdf("tupleudf", br("bcr_patient_uuid"), otp, Nil, "hint test")),
      "aliquot" -> br("bcr_aliquot_uuid"))))

  // tupleudf takes a string (pid) and returns a tuple (one := pid, two := pid)

  val program = Program(Assignment(name, query))

}

object BagInputUDFExample extends DriverGene {

  // need to change this to your loadTables if you want to use this function
  override def loadTables(shred: Boolean = false, skew: Boolean = false): String =
    if (shred){
      s"""|val samples = spark.table("samples")
          |val IBag_samples__D = samples
          |IBag_samples__D.cache; IBag_samples__D.count
          |
          |val copynumber = spark.table("copynumber")
          |val IBag_copynumber__D = copynumber
          |IBag_copynumber__D.cache; IBag_copynumber__D.count
          |
          |""".stripMargin
    }else{
      s"""|val samples = spark.table("samples")
          |
          |val copynumber = spark.table("copynumber")
          |""".stripMargin
    }

  // name to identify your query
  val name = "BagInputUDF"

  // a map of input types for the parser
  
  val tbls = Map("copynumber" -> copynum.tp, 
                  "samples" -> samples.tp)

  val udfTypes = Map("identityudf" -> BagType(TupleType("bcr_patient_uuid" -> StringType, 
                                    "cnvs" -> BagType(TupleType("cn_gene_id" -> StringType, 
                                                                "cnum" -> DoubleType)))))


  val query = 
   s"""
    FirstInput <=
      for s in samples union 
        {( bcr_patient_uuid := s.bcr_patient_uuid, cnvs := 
          (for c in copynumber union 
            if (s.bcr_aliquot_uuid = c.cn_aliquot_uuid)
            then {( cn_gene_id := c.cn_gene_id, cnum := c.cn_copy_number + 0.001 )}).sumBy({cn_gene_id},{cnum})
        )};

    Output <= identityudf(FirstInput)

   """

  val parser = Parser(tbls, udfTypes)
  val program = parser.parse(query).get.asInstanceOf[Program]

}

object BagCountUDFExample extends DriverGene {

  // need to change this to your loadTables if you want to use this function
  override def loadTables(shred: Boolean = false, skew: Boolean = false): String =
    if (shred){
      s"""|val samples = spark.table("samples")
          |val IBag_samples__D = samples
          |IBag_samples__D.cache; IBag_samples__D.count
          |
          |val copynumber = spark.table("copynumber")
          |val IBag_copynumber__D = copynumber
          |IBag_copynumber__D.cache; IBag_copynumber__D.count
          |
          |""".stripMargin
    }else{
      s"""|val samples = spark.table("samples")
          |
          |val copynumber = spark.table("copynumber")
          |""".stripMargin
    }

  // name to identify your query
  val name = "BagCountUDF"

  // a map of input types for the parser
  
  val tbls = Map("copynumber" -> copynum.tp, 
                  "samples" -> samples.tp)

  val udfTypes = Map("countudf" -> LongType)


  val query = 
   s"""
    FirstInput <=
      for s in samples union 
        {( bcr_patient_uuid := s.bcr_patient_uuid, cnvs := 
          (for c in copynumber union 
            if (s.bcr_aliquot_uuid = c.cn_aliquot_uuid)
            then {( cn_gene_id := c.cn_gene_id, cnum := c.cn_copy_number + 0.001 )}).sumBy({cn_gene_id},{cnum})
        )};

    Output <= countudf(FirstInput)

   """

  val parser = Parser(tbls, udfTypes)
  val program = parser.parse(query).get.asInstanceOf[Program]

}

object PivotUDFExample extends DriverGene {

  val sampleFile = "/mnt/app_hdd/data/biospecimen/aliquot/nationwidechildrens.org_biospecimen_aliquot_prad.txt"
  val cnvFile = "/mnt/app_hdd/data/cnv"
  val exprFile = "/mnt/app_hdd/data/expression/"
  val aexprFile = "/mnt/app_hdd/data/fpkm_uq_case_aliquot.txt"
  val occurFile = "/mnt/app_hdd/data/somatic/"
  // For the tcga loader
  val occurName = "datasetFull"
  val occurDicts = ("odictMutect1", "odictMutect2", "odictMutect3")
  // val occurDicts = ("cancerDict1", "cancerDict2", "cancerDict3")
  val pathFile = "/mnt/app_hdd/data/pathway/c2.cp.v7.1.symbols.gmt"
  val gtfFile = "/mnt/app_hdd/data/genes/Homo_sapiens.GRCh37.87.chr.gtf"
  val pradFile = "/mnt/app_hdd/data/biospecimen/clinical/nationwidechildrens.org_clinical_patient_prad.txt"
  val clinDir = "/mnt/app_hdd/data/biospecimen/clinical"

  override def loadTables(shred: Boolean = false, skew: Boolean = false): String =
    s"""|val tcgaLoader = new TCGALoader(spark)
        |val tcgaData = tcgaLoader.load("/mnt/app_hdd/data/biospecimen/clinical", dir = true)
        |${loadBiospec(shred, skew, fname = sampleFile, name = "samples")}
        |${loadCopyNumber(shred, skew, fname = cnvFile)}
        |${loadOccurrence(shred, skew, fname = occurFile, iname = occurName, dictNames = occurDicts)}
        |${loadPathway(shred, skew, fname = pathFile)}
        |${loadGtfTable(shred, skew, fname = gtfFile)}
        |""".stripMargin
  // name to identify your query
  val name = "MulticlassUDF"

  // a map of input types for the parser
  val tbls = Map("occurrences" -> occurrences.tp,
    "copynumber" -> copynum.tp,
    "samples" -> samples.tp,
    "pathways" -> pathway.tp,
    "clinical" -> BagType(tcgaType),// this is for the tcga only
    "genemap" -> gtf.tp)
  //    "clinical" -> BagType(pradType))

  val udfTypes = Map("pivotudf" -> PythonType)

  val query =

    s"""   FirstInput <=
               for g in genemap union
                 {(gene:= g.g_gene_name, burdens :=
                   (for o in occurrences union
                     for s in clinical union
                      if (o.donorId = s.sample) then
                        for t in o.transcript_consequences union
                          if (g.g_gene_id = t.gene_id) then
                              {(sid := o.donorId,
                              lbl := s.tumor_tissue_site,
                              burden := if (t.impact = "HIGH") then 0.80
                                                     else if (t.impact = "MODERATE") then 0.50
                                                     else if (t.impact = "LOW") then 0.30
                                                     else 0.01)}).sumBy({sid, lbl}, {burden}))};

               Output <= pivotudf(FirstInput, {sid, lbl, _1,burden, ANOVA}, {chisq})

           """

  // finally define the parser, note that it takes the input types
  // map as input and pass the query string to the parser to
  // generate the program.
  val parser = Parser(tbls, udfTypes)
  val program = parser.parse(query).get.asInstanceOf[Program]

}

object HintUDFExample extends DriverGene {

  val sampleFile = "/mnt/app_hdd/data/biospecimen/aliquot/nationwidechildrens.org_biospecimen_aliquot_prad.txt"
  val cnvFile = "/mnt/app_hdd/data/cnv"
  val exprFile = "/mnt/app_hdd/data/expression/"
  val aexprFile = "/mnt/app_hdd/data/fpkm_uq_case_aliquot.txt"
  val occurFile = "/mnt/app_hdd/data/somatic/"
  // For the tcga loader
  val occurName = "datasetFull"
  val occurDicts = ("odictMutect1", "odictMutect2", "odictMutect3")
  // val occurDicts = ("cancerDict1", "cancerDict2", "cancerDict3")
  val pathFile = "/mnt/app_hdd/data/pathway/c2.cp.v7.1.symbols.gmt"
  val gtfFile = "/mnt/app_hdd/data/genes/Homo_sapiens.GRCh37.87.chr.gtf"
  val pradFile = "/mnt/app_hdd/data/biospecimen/clinical/nationwidechildrens.org_clinical_patient_prad.txt"
  val clinDir = "/mnt/app_hdd/data/biospecimen/clinical"

  override def loadTables(shred: Boolean = false, skew: Boolean = false): String =
    s"""|val tcgaLoader = new TCGALoader(spark)
        |val tcgaData = tcgaLoader.load("/mnt/app_hdd/data/biospecimen/clinical", dir = true)
        |${loadBiospec(shred, skew, fname = sampleFile, name = "samples")}
        |${loadCopyNumber(shred, skew, fname = cnvFile)}
        |${loadOccurrence(shred, skew, fname = occurFile, iname = occurName, dictNames = occurDicts)}
        |${loadPathway(shred, skew, fname = pathFile)}
        |${loadGtfTable(shred, skew, fname = gtfFile)}
        |""".stripMargin
  // name to identify your query
  val name = "HintUdfQuery"

  // a map of input types for the parser
  val tbls = Map("occurrences" -> occurrences.tp,
    "copynumber" -> copynum.tp,
    "samples" -> samples.tp,
    "pathways" -> pathway.tp,
    "clinical" -> BagType(tcgaType),// this is for the tcga only
    "genemap" -> gtf.tp)
  //    "clinical" -> BagType(pradType))

  val udfTypes = Map("pivotudf" -> PythonType)

  val query =

    s"""   FirstInput <=
               for g in genemap union
                 {(gene:= g.g_gene_name, burdens :=
                   (for o in occurrences union
                     for s in clinical union
                      if (o.donorId = s.sample) then
                        for t in o.transcript_consequences union
                          if (g.g_gene_id = t.gene_id) then
                              {(sid := o.donorId,
                              lbl := s.tumor_tissue_site,
                              burden := if (t.impact = "HIGH") then 0.80
                                                     else if (t.impact = "MODERATE") then 0.50
                                                     else if (t.impact = "LOW") then 0.30
                                                     else 0.01)}).sumBy({sid, lbl}, {burden}))};

               Output <= pivotudf(FirstInput, {sid, lbl, _1,burden}, {chisq})

           """

  // finally define the parser, note that it takes the input types
  // map as input and pass the query string to the parser to
  // generate the program.
  val parser = Parser(tbls, udfTypes)
  val program = parser.parse(query).get.asInstanceOf[Program]

}

