package src.main;

import chemaxon.checkers.ValenceErrorChecker;
import chemaxon.checkers.ValencePropertyChecker;
import chemaxon.checkers.result.StructureCheckerResult;
import chemaxon.fixers.RemoveValencePropertyFixer;
import chemaxon.fixers.StructureFixer;
import chemaxon.formats.MolExporter;
import chemaxon.formats.MolImporter;
import chemaxon.standardizer.Standardizer;
import chemaxon.struc.DPoint3;
import chemaxon.struc.Molecule;
import chemaxon.struc.RxnMolecule;
import com.chemaxon.mapper.AutoMapper;
import com.chemaxon.mapper.Mapper;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


class Utils {
    static Molecule getMolFromInputStream(InputStream is) {
        Molecule mol = null;
        try (MolImporter mi = new MolImporter(is)) {
            mol = mi.read();
        } catch (IOException e) {
            e.printStackTrace();
            return mol;
        }
        return mol;
    }

    static void mapReaction(RxnMolecule rxn, Mapper.MappingStyle ms) {
        AutoMapper mapper = new AutoMapper();
        mapper.setMappingStyle(ms);
        mapper.map(rxn);
    }

    static void parseArgument(Object instance, String[] args) {
        CmdLineParser parser = new CmdLineParser(instance);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.out.println("USAGE: (All arguments is required.)");
            System.out.println();
            parser.printUsage(System.out);
            System.exit(1);
        }
    }

    static Boolean checkMwOfProductsInReaction(RxnMolecule reaction) throws IOException {
        double threshold = 1000;
        List<Boolean> booleanList = new ArrayList<>();
        for (int i = 0; i < reaction.getProductCount(); i++) {
            booleanList.add(reaction.getProduct(i).getMass() > threshold);
        }
        return booleanList.contains(true);
    }

    static Boolean isOverMaxAtomCountOfProductsInReaction(RxnMolecule reaction, int threshold) throws IOException {
        if (threshold == 0) {
            return false;
        }
        List<Boolean> boolList = new ArrayList<>();
        for (int i = 0; i < reaction.getProductCount(); i++) {
            boolList.add(reaction.getProduct(i).getAtomCount() > threshold);
        }
        return boolList.contains(true);
    }

    static Boolean standardizeReaction(RxnMolecule reaction, Standardizer std) {
        int j = reaction.getComponentCount(RxnMolecule.AGENTS);
        for (int i = 0; i < j; i++) {
            reaction.removeComponent(RxnMolecule.AGENTS, 0);
        }
        try {
            for (int i = 0; i < reaction.getReactantCount(); i++) {
                Molecule reactant = reaction.getReactant(i);
                std.standardize(reactant);
            }
            for (int i = 0; i < reaction.getProductCount(); i++) {
                Molecule product = reaction.getProduct(i);
                std.standardize(product);
            }
        } catch (NullPointerException e) {
            return false;
        }
        reaction.removeEmptyComponents();
        return true;
    }

    static RxnMolecule sortReactantsInReaction(RxnMolecule reaction){
        RxnMolecule sortedReaction = new RxnMolecule();
        DPoint3[] reactionArrow = reaction.getReactionArrow();
        sortedReaction.setReactionArrow(reactionArrow);
        sortedReaction.addComponent(reaction.getProduct(0), RxnMolecule.PRODUCTS);
        Map<Integer, Molecule> tMap = new TreeMap<>();
        for (Molecule m : reaction.getReactants()) {
            tMap.put(m.getAtomCount(), m);
        }
        for (Integer nKey : tMap.keySet()) {
            sortedReaction.addComponent(tMap.get(nKey), RxnMolecule.REACTANTS);
        }
        return sortedReaction;
    }

    static Boolean isInvalidReaction(RxnMolecule reaction) {
        return reaction.getProductCount() != 1 | reaction.getReactantCount() > 3 | reaction.getReactantCount() == 0;
    }

    static Boolean isEmptyProductInReaction(RxnMolecule reaction) {
        return reaction.getProduct(0).isEmpty();
    }

    static Boolean isValidValence(RxnMolecule reaction) {
        ValenceErrorChecker checker = new ValenceErrorChecker();
        StructureCheckerResult result = checker.check(reaction);
        return result == null;
    }

    static Boolean checkAndFixValenceProperty(RxnMolecule reaction) throws IOException{
        ValencePropertyChecker vpc = new ValencePropertyChecker();
        StructureCheckerResult result = null;
        try {
            result = vpc.check(reaction);
        } catch (ArrayIndexOutOfBoundsException ignored) {  // ANY atom
        }
        boolean bool = true;
        if (result != null) {
            StructureFixer fixer = new RemoveValencePropertyFixer();
            bool = fixer.fix(result);
        }
        return bool;
    }

    static RxnMolecule splitComponent(RxnMolecule reaction, String format) throws IOException {
        String sMol = MolExporter.exportToFormat(reaction, format);
        return RxnMolecule.getReaction(MolImporter.importMol(sMol));
    }
}
