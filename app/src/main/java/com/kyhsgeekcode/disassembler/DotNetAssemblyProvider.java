package com.kyhsgeekcode.disassembler;

import at.pollaknet.api.facile.symtab.symbols.Type;
import at.pollaknet.api.facile.symtab.symbols.scopes.Assembly;

// Normal disassembly and dotnet disassembly are different
// Address -> index of method or (address->method를 찾는 기능 만들기)
// size -> 1 or (실제 바이트 사이즈)
//
public class DotNetAssemblyProvider extends AssemblyProvider {
    public DotNetAssemblyProvider(MainActivity activity, DisasmListViewAdapter adapter, long total, Assembly assembly) {
        super(activity, adapter, total);
        this.assembly = assembly;
        Type[] types = assembly.getAllTypes();
        for (Type t : types) {
            //t.get
        }
    }

    //Not implemented
    @Override
    public long getAll(byte[] bytes, long offset, long size, long virtaddr) {
        return 0;
    }

    // Should Implement
    @Override
    public long getSome(byte[] bytes, long offset, long size, long virtaddr, int count) {

        return 0;
    }

    Assembly assembly;
}
